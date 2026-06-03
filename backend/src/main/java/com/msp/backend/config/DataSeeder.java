package com.msp.backend.config;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.merchant.MerchantUserMapping;
import com.msp.backend.modules.merchant.MerchantUserMappingRepository;
import com.msp.backend.modules.analytics.Analytics;
import com.msp.backend.modules.analytics.AnalyticsRepository;
import com.msp.backend.modules.role.Permission;
import com.msp.backend.modules.role.PermissionRepository;
import com.msp.backend.modules.role.Role;
import com.msp.backend.modules.role.RolePermission;
import com.msp.backend.modules.role.RolePermissionRepository;
import com.msp.backend.modules.role.RoleRepository;
import com.msp.backend.modules.settlement.CreditAdvice;
import com.msp.backend.modules.settlement.CreditAdviceRepository;
import com.msp.backend.modules.settlement.Settlement;
import com.msp.backend.modules.settlement.SettlementRepository;
import com.msp.backend.modules.transaction.Refund;
import com.msp.backend.modules.transaction.RefundRepository;
import com.msp.backend.modules.transaction.Transaction;
import com.msp.backend.modules.transaction.TransactionRepository;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserRole;
import com.msp.backend.modules.user.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantUserMappingRepository merchantUserMappingRepository;
    private final TransactionRepository transactionRepository;
    private final RefundRepository refundRepository;
    private final SettlementRepository settlementRepository;
    private final CreditAdviceRepository creditAdviceRepository;
    private final AnalyticsRepository analyticsRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private final Random random = new Random();

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            seedAll();
        } else {
            log.info("Database already contains data. Skipping initial seeding.");
        }
        // Seed recent data for Apr 26 – May 6 2026 (runs once, guarded by sentinel)
        seedRecentData();
    }

    public void reseed() {
        log.info("=== Clearing existing data ===");
        analyticsRepository.deleteAll();
        refundRepository.deleteAll();
        transactionRepository.deleteAll();
        settlementRepository.deleteAll();
        creditAdviceRepository.deleteAll();
        merchantUserMappingRepository.deleteAll(); // clear mappings before merchants
        userRoleRepository.deleteAll();
        rolePermissionRepository.deleteAll();
        permissionRepository.deleteAll();
        merchantRepository.deleteAll();   // must delete before users (FK: merchant → user)
        userRepository.deleteAll();
        roleRepository.deleteAll();
        seedAll();
    }

    private void seedAll() {
        log.info("=== Starting Database Seeding ===");

        List<Role> roles = seedRoles();
        List<Permission> permissions = seedPermissions();
        seedRolePermissions(roles, permissions);
        List<Merchant> merchants = seedMerchants();
        List<User> users = seedUsers(roles);
        linkMerchantsToUsers(merchants, users);
        List<Transaction> transactions = seedTransactions(merchants);
        List<CreditAdvice> creditAdvices = seedCreditAdvices(merchants);
        List<Settlement> settlements = seedSettlements(creditAdvices, transactions);
        updateCreditAdviceAmounts(creditAdvices, settlements);
        seedRefunds(transactions, merchants);
        seedAnalytics(merchants, transactions);

        log.info("=== Database Seeding Complete ===");
        log.info("Login credentials:");
        log.info("  Admin: admin@msp.com / admin123");
        log.info("  Merchant: john.tech@example.com / merchant123");
    }

    // ── JSON helpers ──────────────────────────────────────────

    private <T> List<T> loadJsonList(String path, TypeReference<List<T>> typeRef) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(is, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load seed data from " + path, e);
        }
    }

    private JsonNode loadJsonNode(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readTree(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load seed data from " + path, e);
        }
    }

    // ── Static data from JSON ─────────────────────────────────

    private List<Role> seedRoles() {
        log.info("Seeding roles...");
        List<Map<String, String>> data = loadJsonList(
                "seed-data/roles.json", new TypeReference<>() {});

        for (Map<String, String> d : data) {
            Role role = new Role();
            role.setRoleName(d.get("roleName"));
            role.setDescription(d.get("description"));
            role.setRoleType(d.get("roleType"));
            roleRepository.save(role);
        }

        List<Role> roles = roleRepository.findAll();
        log.info("Created {} roles", roles.size());
        return roles;
    }

    private List<Permission> seedPermissions() {
        log.info("Seeding permissions...");
        List<Map<String, String>> data = loadJsonList(
                "seed-data/permissions.json", new TypeReference<>() {});

        List<Permission> permissions = new ArrayList<>();
        for (Map<String, String> d : data) {
            Permission perm = new Permission();
            perm.setPermissionName(d.get("permissionName"));
            perm.setDescription(d.get("description"));
            perm.setModule(d.get("module"));
            permissions.add(permissionRepository.save(perm));
        }

        log.info("Created {} permissions", permissions.size());
        return permissions;
    }

    private void seedRolePermissions(List<Role> roles, List<Permission> permissions) {
        log.info("Seeding role-permissions...");
        JsonNode mapping = loadJsonNode("seed-data/role-permissions.json");

        mapping.properties().forEach(entry -> {
            String roleName = entry.getKey();
            Role role = roles.stream()
                    .filter(r -> roleName.equals(r.getRoleName()))
                    .findFirst().orElseThrow();

            List<String> permNames = new ArrayList<>();
            entry.getValue().forEach(node -> permNames.add(node.textValue()));

            boolean allPerms = permNames.contains("ALL");
            List<Permission> toAssign = allPerms
                    ? permissions
                    : permissions.stream()
                        .filter(p -> permNames.contains(p.getPermissionName()))
                        .toList();

            for (Permission p : toAssign) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(role.getRoleId());
                rp.setPermissionId(p.getPermissionId());
                rp.setCreatedBy("SYSTEM");
                rp.setLastModifiedBy("SYSTEM");
                rp.setLastModifiedAt(LocalDateTime.now());
                rolePermissionRepository.save(rp);
            }
        });

        log.info("Role permissions assigned");
    }

    private List<Merchant> seedMerchants() {
        log.info("Seeding merchants...");
        List<Map<String, String>> data = loadJsonList(
                "seed-data/merchants.json", new TypeReference<>() {});

        for (Map<String, String> d : data) {
            Merchant m = new Merchant();
            m.setMerchantName(d.get("merchantName"));
            m.setContact(d.get("contact"));
            m.setAddressLine1(d.get("addressLine1"));
            m.setAddressLine2(d.get("addressLine2"));
            m.setPostcode(d.get("postcode"));
            m.setCity(d.get("city"));
            m.setCountry(d.get("country"));
            m.setStatus(d.get("status"));
            merchantRepository.save(m);
        }

        List<Merchant> merchants = merchantRepository.findAll();
        log.info("Created {} merchants", merchants.size());
        return merchants;
    }

    private List<User> seedUsers(List<Role> roles) {
        log.info("Seeding users...");
        List<JsonNode> data = loadJsonList(
                "seed-data/users.json", new TypeReference<>() {});

        List<User> users = new ArrayList<>();
        for (JsonNode d : data) {
            User u = new User();
            u.setEmail(d.get("email").textValue());
            u.setPassword(passwordEncoder.encode(d.get("password").textValue()));
            u.setFirstName(d.get("firstName").textValue());
            u.setLastName(d.get("lastName").textValue());
            u.setDisplayName(d.get("displayName").textValue());
            u.setContactNumber(d.get("contactNumber").textValue());
            u.setStatus(d.get("status").textValue());
            u.setMfaEnabled(d.get("mfaEnabled").booleanValue());
            u.setMustChangePassword(d.get("mustChangePassword").booleanValue());

            User saved = userRepository.save(u);
            users.add(saved);

            // Assign role from JSON
            String roleName = d.get("role").textValue();
            Role role = roles.stream()
                    .filter(r -> roleName.equals(r.getRoleName()))
                    .findFirst().orElseThrow();

            UserRole ur = new UserRole();
            ur.setUserId(saved.getUserId());
            ur.setRoleId(role.getRoleId());
            ur.setCreatedBy("SYSTEM");
            ur.setLastModifiedBy("SYSTEM");
            ur.setLastModifiedAt(LocalDateTime.now());
            userRoleRepository.save(ur);
        }

        log.info("Created {} users", users.size());
        return users;
    }

    // ── Dynamic data (generated) ──────────────────────────────

    private void linkMerchantsToUsers(List<Merchant> merchants, List<User> users) {
        log.info("Linking merchants to users...");
        List<User> merchantUsers = users.stream()
                .filter(u -> !"admin@msp.com".equals(u.getEmail())
                           && !"manager@msp.com".equals(u.getEmail()))
                .toList();

        for (int i = 0; i < merchants.size() && i < merchantUsers.size(); i++) {
            Merchant m = merchants.get(i);
            User u = merchantUsers.get(i);

            // Seed into merchant_users junction table
            MerchantUserMapping mapping = new MerchantUserMapping();
            mapping.setMerchantId(m.getMerchantId());
            mapping.setUserId(u.getUserId());
            mapping.setCreatedBy("SYSTEM");
            mapping.setLastModifiedBy("SYSTEM");
            merchantUserMappingRepository.save(mapping);
        }
        log.info("Merchants linked to users");
    }

    private List<Transaction> seedTransactions(List<Merchant> merchants) {
        log.info("Seeding transactions...");
        List<JsonNode> data = loadJsonList(
                "seed-data/transactions.json", new TypeReference<>() {});

        // ── Spread transactions across the last 180 days for AI analytics ──
        // We repeat the JSON entries multiple times with randomised dates so that
        // XGBoost churn, Prophet forecasting, and RFM segmentation have enough
        // date-range diversity to train properly.
        LocalDateTime now = LocalDateTime.now();
        int daysSpread = 730;   // 2 years of history
        int repeats    = 12;    // multiply data volume ×12 (~3000 txns)

        // Cards that will appear ONLY >90 days ago (simulate churned customers)
        java.util.Set<String> churnedCards = java.util.Set.of(
                "4485770012345612", "4539667788003349", "5102991144567734");

        // Cards that will ONLY appear in the last 60 days (new customers)
        java.util.Set<String> newCards = java.util.Set.of(
                "4024007123456003", "4012334477890056");

        // Extra payment channels and currencies for realism
        String[] extraChannels = {"CARD", "ONLINE", "E_WALLET", "QR_PAY", "BANK_TRANSFER"};
        String[] currencies = {"MYR"};

        List<Transaction> allTransactions = new ArrayList<>();
        int refSeq = 500000;    // unique refNo counter

        for (int round = 0; round < repeats; round++) {
            for (int i = 0; i < data.size(); i++) {
                JsonNode d = data.get(i);
                Merchant merchant = merchants.get(i % merchants.size());
                String cardNo = d.get("cardNo").textValue();

                Transaction t = new Transaction();
                t.setMerchantId(merchant.getMerchantId());
                t.setRefNo(String.valueOf(refSeq++));
                t.setCardNo(cardNo);
                t.setTxnDescription(d.get("txnDescription").textValue());

                // Slightly vary the amount each round (±20 %) for realism
                BigDecimal baseAmount = d.get("amount").decimalValue();
                double factor = 0.80 + random.nextDouble() * 0.40;  // 0.80 – 1.20
                BigDecimal amount = baseAmount.multiply(BigDecimal.valueOf(factor))
                        .setScale(2, RoundingMode.HALF_UP);
                t.setAmount(amount);
                t.setDiscountAmount(BigDecimal.ZERO);
                t.setNettAmount(amount);

                // Add currency and channel variety
                t.setCurrency(currencies[random.nextInt(currencies.length)]);
                t.setPaymentChannel(extraChannels[random.nextInt(extraChannels.length)]);

                // ~8 % of transactions are DECLINED for realism
                String status = d.get("status").textValue();
                if ("APPROVED".equals(status) && random.nextInt(100) < 8) {
                    status = "DECLINED";
                }
                t.setStatus(status);

                // Determine date range based on card behaviour
                int daysAgo;
                if (churnedCards.contains(cardNo)) {
                    daysAgo = 91 + random.nextInt(daysSpread - 91); // only old (>90 days), up to 2 years
                } else if (newCards.contains(cardNo)) {
                    daysAgo = random.nextInt(60);                   // only recent (<60 days)
                } else {
                    // spread evenly across full 2-year window; bias 75% to >90 days for churn model
                    daysAgo = random.nextDouble() < 0.75
                            ? 91 + random.nextInt(daysSpread - 91)  // historical (>90 days)
                            : random.nextInt(90);                   // recent (≤90 days)
                }
                int hour    = 7 + random.nextInt(13); // 07:00 – 19:59
                int minute  = random.nextInt(60);
                LocalDateTime txnDate = now.minusDays(daysAgo)
                        .withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                t.setTxnDate(txnDate);

                if ("APPROVED".equals(t.getStatus())) {
                    t.setPostedDate(txnDate.plusDays(1 + random.nextInt(3)));
                }

                allTransactions.add(transactionRepository.save(t));
            }
        }

        log.info("Created {} transactions ({}× base data spread over {} days)",
                allTransactions.size(), repeats, daysSpread);

        // ── Synthetic transactions: 500 unique cards × 2 years ──────────────
        // Creates 4 customer archetypes to give the churn model realistic variety:
        //   A. Loyal (35%) — high frequency, low recency, appears in both windows
        //   B. Churned (30%) — was active historically, disappeared in last 90 days
        //   C. At-Risk (20%) — declining frequency, sporadic recent activity
        //   D. New (15%) — only recent transactions, no historical data
        log.info("Seeding synthetic transactions for AI model diversity...");
        String[] syntheticDescs = {
            "ONLINE PURCHASE", "RETAIL PAYMENT", "SUBSCRIPTION FEE",
            "FOOD & BEVERAGE", "TRANSPORT", "UTILITY BILL", "ENTERTAINMENT",
            "GROCERY", "FASHION", "ELECTRONICS"
        };
        int synthSeq = 900000;

        for (int c = 1; c <= 500; c++) {
            String syntheticCard = String.format("9%015d", c);
            Merchant merchant = merchants.get(c % merchants.size());
            double archetype = random.nextDouble();

            int perCard;
            boolean historicalOnly;  // true = churned customer (no recent txns)
            boolean recentOnly;      // true = new customer (no historical txns)
            double avgAmount;
            int minDaysAgo, maxDaysAgo;

            if (archetype < 0.35) {
                // ── A. Loyal customer: many transactions across full 2 years ──
                perCard = 15 + random.nextInt(25);  // 15-39 txns
                avgAmount = 50 + random.nextDouble() * 400;
                historicalOnly = false;
                recentOnly = false;
                minDaysAgo = 0;
                maxDaysAgo = 730;
            } else if (archetype < 0.65) {
                // ── B. Churned customer: ONLY historical (>90 days ago) ──
                perCard = 3 + random.nextInt(12);   // 3-14 txns
                avgAmount = 20 + random.nextDouble() * 200;
                historicalOnly = true;
                recentOnly = false;
                minDaysAgo = 91;
                maxDaysAgo = 730;
            } else if (archetype < 0.85) {
                // ── C. At-risk: mostly historical, maybe 1-2 recent ──
                perCard = 5 + random.nextInt(10);   // 5-14 txns
                avgAmount = 30 + random.nextDouble() * 300;
                historicalOnly = false;
                recentOnly = false;
                minDaysAgo = 0;
                maxDaysAgo = 730;
            } else {
                // ── D. New customer: recent only (<90 days) ──
                perCard = 2 + random.nextInt(6);    // 2-7 txns
                avgAmount = 30 + random.nextDouble() * 350;
                historicalOnly = false;
                recentOnly = true;
                minDaysAgo = 0;
                maxDaysAgo = 89;
            }

            for (int t = 0; t < perCard; t++) {
                Transaction tx = new Transaction();
                tx.setMerchantId(merchant.getMerchantId());
                tx.setRefNo(String.valueOf(synthSeq++));
                tx.setCardNo(syntheticCard);
                tx.setTxnDescription(syntheticDescs[random.nextInt(syntheticDescs.length)]);

                // Vary amounts: ±50% from archetype avg for realistic spread
                double factor = 0.50 + random.nextDouble() * 1.00;
                tx.setAmount(BigDecimal.valueOf(avgAmount * factor)
                        .setScale(2, RoundingMode.HALF_UP));
                tx.setDiscountAmount(BigDecimal.ZERO);
                tx.setNettAmount(tx.getAmount());
                tx.setCurrency(currencies[random.nextInt(currencies.length)]);
                tx.setPaymentChannel(extraChannels[random.nextInt(extraChannels.length)]);
                // 90% APPROVED, 5% DECLINED, 5% REFUNDED
                double statusRoll = random.nextDouble();
                tx.setStatus(statusRoll < 0.90 ? "APPROVED" : statusRoll < 0.95 ? "DECLINED" : "REFUNDED");

                int daysAgo;
                if (historicalOnly) {
                    // Only old transactions (churned)
                    daysAgo = minDaysAgo + random.nextInt(maxDaysAgo - minDaysAgo + 1);
                } else if (recentOnly) {
                    // Only recent transactions (new customer)
                    daysAgo = random.nextInt(maxDaysAgo + 1);
                } else if (archetype >= 0.65 && archetype < 0.85) {
                    // At-risk: 85% historical, 15% recent
                    daysAgo = random.nextDouble() < 0.85
                            ? 91 + random.nextInt(639)
                            : random.nextInt(90);
                } else {
                    // Loyal: spread evenly across full range
                    daysAgo = random.nextInt(maxDaysAgo + 1);
                }

                int hour   = 7 + random.nextInt(13);
                int minute = random.nextInt(60);
                LocalDateTime txnDate = now.minusDays(daysAgo)
                        .withHour(hour).withMinute(minute).withSecond(0).withNano(0);
                tx.setTxnDate(txnDate);
                if ("APPROVED".equals(tx.getStatus())) {
                    tx.setPostedDate(txnDate.plusDays(1 + random.nextInt(3)));
                }
                allTransactions.add(transactionRepository.save(tx));
            }
        }
        log.info("Total transactions after synthetic generation: {}", allTransactions.size());
        return allTransactions;
    }

    private void seedRefunds(List<Transaction> transactions, List<Merchant> merchants) {
        log.info("Seeding refunds...");
        List<JsonNode> data = loadJsonList(
                "seed-data/refunds.json", new TypeReference<>() {});

        // Also randomly pick ~5% of APPROVED transactions for additional refunds
        List<Transaction> approvedTxns = transactions.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()))
                .toList();

        int refundCount = 0;
        int refundSeq = 100;

        // 1) Seed refunds from JSON template (linked to specific transactions)
        for (JsonNode d : data) {
            int txnIdx = d.get("transactionIndex").intValue();
            if (txnIdx >= transactions.size()) continue;

            Transaction t = transactions.get(txnIdx);

            Refund r = new Refund();
            r.setTransactionId(t.getTransactionId());
            r.setMerchantId(t.getMerchantId());
            r.setCardNo(t.getCardNo());
            r.setCurrency(t.getCurrency());
            r.setAmount(t.getAmount());

            String refundType = d.get("refundType").textValue();
            r.setRefundType(refundType);

            if ("FULL".equals(refundType)) {
                r.setRefundAmount(t.getAmount());
            } else {
                int pct = d.has("refundPercent") ? d.get("refundPercent").intValue() : 50;
                BigDecimal refundAmount = t.getAmount()
                        .multiply(BigDecimal.valueOf(pct))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                r.setRefundAmount(refundAmount);
            }

            r.setRefundRefNo(d.get("refundRefNo").textValue());
            String status = d.get("status").textValue();
            r.setStatus(status);
            r.setTransactionDate(t.getTxnDate());
            // Derive submission date relative to the transaction date (2–7 days later)
            r.setSubmissionDate(t.getTxnDate().plusDays(2 + random.nextInt(6)));

            if ("APPROVED".equals(r.getStatus())) {
                r.setPostedDate(r.getSubmissionDate().plusDays(2 + random.nextInt(3)));
                // Mark the original transaction as REFUNDED but keep it in transactions table
                t.setStatus("REFUNDED");
                transactionRepository.save(t);
            }

            refundRepository.save(r);
            refundCount++;
        }

        // 2) Generate additional random refunds (~5% of approved transactions)
        for (Transaction t : approvedTxns) {
            if (random.nextInt(100) >= 5) continue; // skip 95%

            Refund r = new Refund();
            r.setTransactionId(t.getTransactionId());
            r.setMerchantId(t.getMerchantId());
            r.setCardNo(t.getCardNo());
            r.setCurrency(t.getCurrency());
            r.setAmount(t.getAmount());

            boolean isFull = random.nextBoolean();
            r.setRefundType(isFull ? "FULL" : "PARTIAL");
            if (isFull) {
                r.setRefundAmount(t.getAmount());
            } else {
                int pct = 20 + random.nextInt(61); // 20-80%
                r.setRefundAmount(t.getAmount()
                        .multiply(BigDecimal.valueOf(pct))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }

            r.setRefundRefNo("RFN" + String.format("%08d", refundSeq++));
            r.setTransactionDate(t.getTxnDate());
            r.setSubmissionDate(t.getTxnDate().plusDays(2 + random.nextInt(6)));

            // 70% approved, 30% pending
            if (random.nextInt(100) < 70) {
                r.setStatus("APPROVED");
                r.setPostedDate(r.getSubmissionDate().plusDays(2 + random.nextInt(3)));
                t.setStatus("REFUNDED");
                transactionRepository.save(t);
            } else {
                r.setStatus("PENDING");
            }

            refundRepository.save(r);
            refundCount++;
        }

        log.info("Created {} refunds", refundCount);
    }

    private List<CreditAdvice> seedCreditAdvices(List<Merchant> merchants) {
        log.info("Seeding credit advices...");
        List<JsonNode> data = loadJsonList(
                "seed-data/credit-advices.json", new TypeReference<>() {});

        LocalDateTime now = LocalDateTime.now();
        List<CreditAdvice> allAdvices = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            JsonNode d = data.get(i);
            int merchantIdx = d.get("merchantIndex").intValue();
            Merchant merchant = merchants.get(merchantIdx);

            CreditAdvice ca = new CreditAdvice();
            ca.setMerchantId(merchant.getMerchantId());
            ca.setAccountNo(d.get("accountNo").textValue());
            ca.setAccountId(d.get("accountId").textValue());
            ca.setCurrency(d.get("currency").textValue());
            ca.setAmount(BigDecimal.ZERO); // will be updated after settlements are seeded
            // Spread payment dates across the last 60 days
            ca.setPaymentDate(now.minusDays(i * 2L + random.nextInt(3)).withHour(10 + random.nextInt(6)).withMinute(0).withSecond(0));

            allAdvices.add(creditAdviceRepository.save(ca));
        }

        log.info("Created {} credit advices", allAdvices.size());
        return allAdvices;
    }

    private List<Settlement> seedSettlements(List<CreditAdvice> creditAdvices, List<Transaction> allTransactions) {
        log.info("Seeding settlements...");
        List<JsonNode> data = loadJsonList(
                "seed-data/settlements.json", new TypeReference<>() {});

        List<Settlement> allSettlements = new ArrayList<>();
        for (JsonNode d : data) {
            int caIdx = d.get("creditAdviceIndex").intValue();
            CreditAdvice ca = creditAdvices.get(caIdx);

            Settlement s = new Settlement();
            s.setCreditAdviceId(ca.getCreditAdviceId());
            s.setSettlementNo(d.get("settlementNo").textValue());
            s.setSettlementType(d.get("settlementType").textValue());
            s.setCurrency(d.get("currency").textValue());
            // Settlement date = 1-3 days before the credit-advice payment date
            s.setSettlementDate(ca.getPaymentDate().minusDays(1 + random.nextInt(3)));

            // Collect referenced transactions
            List<Transaction> settlementTxns = new ArrayList<>();
            JsonNode txnIndexes = d.get("transactionIndexes");
            if (txnIndexes != null && txnIndexes.isArray()) {
                for (JsonNode idx : txnIndexes) {
                    int txnIdx = idx.intValue();
                    if (txnIdx < allTransactions.size()) {
                        settlementTxns.add(allTransactions.get(txnIdx));
                    }
                }
            }

            // Settlement amount = sum of linked transaction nett amounts
            BigDecimal settlementAmount = settlementTxns.stream()
                    .map(t -> t.getNettAmount() != null ? t.getNettAmount() : t.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            s.setSettlementAmount(settlementAmount);
            s.setPaymentAmount(settlementAmount.multiply(BigDecimal.valueOf(0.975))
                    .setScale(2, RoundingMode.HALF_UP));

            Settlement saved = settlementRepository.save(s);
            allSettlements.add(saved);

            // Link transactions back to this settlement
            for (Transaction t : settlementTxns) {
                t.setSettlementId(saved.getSettlementId());
                transactionRepository.save(t);
            }
        }

        log.info("Created {} settlements", allSettlements.size());
        return allSettlements;
    }

    private void updateCreditAdviceAmounts(List<CreditAdvice> creditAdvices, List<Settlement> settlements) {
        log.info("Updating credit advice amounts from settlements...");
        // credit advice amount = sum of all its settlements' settlementAmount
        for (CreditAdvice ca : creditAdvices) {
            BigDecimal total = settlements.stream()
                    .filter(s -> ca.getCreditAdviceId().equals(s.getCreditAdviceId()))
                    .map(Settlement::getSettlementAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            ca.setAmount(total);
            creditAdviceRepository.save(ca);
        }
        log.info("Credit advice amounts updated");
    }

    private void seedAnalytics(List<Merchant> merchants, List<Transaction> transactions) {
        log.info("Seeding analytics records...");
        int count = 0;
        String[] metrics = {"TOTAL_SALES", "TOTAL_TRANSACTIONS", "DECLINE_RATE", "AVG_TRANSACTION", "NET_REVENUE", "REFUND_RATE"};

        for (Merchant merchant : merchants) {
            List<Transaction> mTxns = transactions.stream()
                    .filter(t -> merchant.getMerchantId().equals(t.getMerchantId()))
                    .toList();

            long approved = mTxns.stream().filter(t -> "APPROVED".equals(t.getStatus())).count();
            long declined = mTxns.stream().filter(t -> "DECLINED".equals(t.getStatus())).count();
            BigDecimal totalSales = mTxns.stream()
                    .filter(t -> "APPROVED".equals(t.getStatus()) && t.getAmount() != null)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgTxn = approved > 0 ? totalSales.divide(BigDecimal.valueOf(approved), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            double declineRate = mTxns.isEmpty() ? 0 : (double) declined / mTxns.size() * 100;

            String[] values = {
                    totalSales.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    String.valueOf(mTxns.size()),
                    String.format("%.2f", declineRate),
                    avgTxn.toPlainString(),
                    totalSales.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    String.format("%.2f", random.nextDouble() * 10)
            };

            for (int i = 0; i < metrics.length; i++) {
                Analytics a = new Analytics();
                a.setMerchantId(merchant.getMerchantId());
                a.setDataName(metrics[i]);
                a.setDataValue(values[i]);
                a.setGeneratedAt(LocalDateTime.now().minusHours(random.nextInt(48)));
                analyticsRepository.save(a);
                count++;
            }
        }
        log.info("Created {} analytics records", count);
    }

    // ══════════════════════════════════════════════════════════════════
    // Delete all records with dates in Apr 26 – May 6, 2026
    // ══════════════════════════════════════════════════════════════════

    private void seedRecentData() {
        // Guard: only seed once
        if (!analyticsRepository.findByDataNameOrderByGeneratedAtDesc("RECENT_SEED_DONE").isEmpty()) {
            log.info("Recent data (Apr 26 – May 6 2026) already seeded. Skipping.");
            return;
        }

        List<Merchant> merchants = merchantRepository.findAll();
        if (merchants.isEmpty()) {
            log.warn("No merchants found. Skipping recent data seed.");
            return;
        }

        LocalDateTime rangeStart = LocalDateTime.of(2026, 4, 26, 0, 0);
        int totalDays = 11; // Apr 26–30 + May 1–6

        String[] channels = {"CARD", "ONLINE", "E_WALLET", "QR_PAY", "BANK_TRANSFER"};
        String[] descs = {
            "POS PURCHASE", "ONLINE ORDER", "SUBSCRIPTION RENEWAL",
            "RETAIL PAYMENT", "FOOD DELIVERY", "TRANSPORT FARE",
            "UTILITY BILL", "GROCERY PURCHASE", "DIGITAL GOODS",
            "SERVICE FEE", "MEMBERSHIP FEE", "INSURANCE PREMIUM"
        };
        String currency = "MYR";

        // Use high starting offsets to avoid collisions with existing records
        int refSeq       = 9_000_000;
        int refundRefSeq = 900_000;
        int settlSeq     = 900_000;
        int caSeq        = 900_000;

        List<Transaction> allNewTxns = new ArrayList<>();

        // ── 1. Generate 20-200 transactions per day ──────────────────────────
        for (int day = 0; day < totalDays; day++) {
            LocalDateTime dayStart = rangeStart.plusDays(day);
            int txnCount = 20 + random.nextInt(181); // 20–200

            for (int t = 0; t < txnCount; t++) {
                Merchant merchant = merchants.get(random.nextInt(merchants.size()));

                Transaction tx = new Transaction();
                tx.setMerchantId(merchant.getMerchantId());
                tx.setRefNo("R" + (refSeq++));
                tx.setCardNo(String.format("4%03d%04d%04d%04d",
                        random.nextInt(1000), random.nextInt(10000),
                        random.nextInt(10000), random.nextInt(10000)));
                tx.setTxnDescription(descs[random.nextInt(descs.length)]);

                BigDecimal amount = BigDecimal.valueOf(10 + random.nextDouble() * 990)
                        .setScale(2, RoundingMode.HALF_UP);
                tx.setAmount(amount);
                tx.setDiscountAmount(BigDecimal.ZERO);
                tx.setNettAmount(amount);
                tx.setCurrency(currency);
                tx.setPaymentChannel(channels[random.nextInt(channels.length)]);

                // Status distribution:
                //   75% APPROVED | 8% DECLINED | 9% REFUNDED | 8% REFUND_REQUESTED
                double roll = random.nextDouble();
                String status;
                if      (roll < 0.75) status = "APPROVED";
                else if (roll < 0.83) status = "DECLINED";
                else if (roll < 0.92) status = "REFUNDED";
                else                  status = "REFUND_REQUESTED";
                tx.setStatus(status);

                int hour   = 7 + random.nextInt(14);
                int minute = random.nextInt(60);
                int second = random.nextInt(60);
                LocalDateTime txnDate = dayStart.withHour(hour).withMinute(minute).withSecond(second);
                tx.setTxnDate(txnDate);

                if ("APPROVED".equals(status) || "REFUNDED".equals(status)) {
                    tx.setPostedDate(txnDate.plusDays(1 + random.nextInt(2)));
                }

                allNewTxns.add(transactionRepository.save(tx));
            }
        }
        log.info("Seeded {} transactions for Apr 26 – May 6 2026", allNewTxns.size());

        // ── 2. Create refunds ─────────────────────────────────────────────────
        // REFUNDED transaction       → refund status APPROVED
        // REFUND_REQUESTED transaction → refund status PENDING
        // CANCELLED refund           → transaction stays APPROVED (no status change)
        int refundCount = 0;

        for (Transaction tx : allNewTxns) {
            if ("REFUNDED".equals(tx.getStatus())) {
                Refund r = buildRefund(tx, "APPROVED", refundRefSeq++);
                r.setPostedDate(r.getSubmissionDate().plusDays(1 + random.nextInt(3)));
                refundRepository.save(r);
                refundCount++;
            } else if ("REFUND_REQUESTED".equals(tx.getStatus())) {
                refundRepository.save(buildRefund(tx, "PENDING", refundRefSeq++));
                refundCount++;
            }
        }

        // ~5% of APPROVED transactions also have a CANCELLED refund
        List<Transaction> approvedTxns = allNewTxns.stream()
                .filter(tx -> "APPROVED".equals(tx.getStatus()))
                .toList();
        int cancelledCount = Math.max(1, approvedTxns.size() / 20);
        for (int i = 0; i < cancelledCount; i++) {
            Transaction tx = approvedTxns.get(random.nextInt(approvedTxns.size()));
            refundRepository.save(buildRefund(tx, "CANCELLED", refundRefSeq++));
            refundCount++;
        }
        log.info("Seeded {} refunds", refundCount);

        // ── 3. Credit advices + settlements for APPROVED transactions only ────
        Map<Long, List<Transaction>> byMerchant = new java.util.LinkedHashMap<>();
        for (Transaction tx : approvedTxns) {
            byMerchant.computeIfAbsent(tx.getMerchantId(), k -> new ArrayList<>()).add(tx);
        }

        int caCount = 0, settlCount = 0;

        for (Map.Entry<Long, List<Transaction>> entry : byMerchant.entrySet()) {
            Long merchantId = entry.getKey();
            List<Transaction> mTxns = new ArrayList<>(entry.getValue());
            mTxns.sort((a, b) -> a.getTxnDate().compareTo(b.getTxnDate()));

            // Batch transactions into ~2-day windows
            List<List<Transaction>> batches = new ArrayList<>();
            List<Transaction> currentBatch = new ArrayList<>();
            LocalDateTime batchCutoff = mTxns.get(0).getTxnDate().plusDays(2);

            for (Transaction tx : mTxns) {
                if (tx.getTxnDate().isAfter(batchCutoff)) {
                    if (!currentBatch.isEmpty()) {
                        batches.add(currentBatch);
                        currentBatch = new ArrayList<>();
                    }
                    batchCutoff = tx.getTxnDate().plusDays(2);
                }
                currentBatch.add(tx);
            }
            if (!currentBatch.isEmpty()) batches.add(currentBatch);

            Merchant merchant = merchants.stream()
                    .filter(m -> m.getMerchantId().equals(merchantId))
                    .findFirst().orElse(merchants.get(0));
            String nameSlug = merchant.getMerchantName()
                    .replaceAll("\\s+", "").toUpperCase();
            String slug = nameSlug.substring(0, Math.min(nameSlug.length(), 6));

            for (List<Transaction> batch : batches) {
                LocalDateTime latestTxn = batch.stream()
                        .map(Transaction::getTxnDate)
                        .max(LocalDateTime::compareTo).orElse(rangeStart);
                LocalDateTime paymentDate = latestTxn
                        .plusDays(3 + random.nextInt(3))
                        .withHour(10 + random.nextInt(4))
                        .withMinute(0).withSecond(0);

                CreditAdvice ca = new CreditAdvice();
                ca.setMerchantId(merchantId);
                ca.setAccountNo("ACC-" + slug + "-" + (caSeq + 1));
                ca.setAccountId("AID" + String.format("%06d", caSeq + 1));
                ca.setCurrency(currency);
                ca.setAmount(BigDecimal.ZERO);
                ca.setPaymentDate(paymentDate);
                CreditAdvice savedCA = creditAdviceRepository.save(ca);
                caSeq++;
                caCount++;

                // Split large batches into 2 settlements, smaller into 1
                int numSettlements = batch.size() > 10 ? 2 : 1;
                int splitPoint     = batch.size() / numSettlements;
                BigDecimal totalCAAmount = BigDecimal.ZERO;

                for (int s = 0; s < numSettlements; s++) {
                    int from = s * splitPoint;
                    int toIdx = (s == numSettlements - 1) ? batch.size() : from + splitPoint;
                    List<Transaction> sTxns = batch.subList(from, toIdx);

                    BigDecimal settlAmt = sTxns.stream()
                            .map(tx -> tx.getNettAmount() != null ? tx.getNettAmount() : tx.getAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);

                    Settlement settlement = new Settlement();
                    settlement.setCreditAdviceId(savedCA.getCreditAdviceId());
                    settlement.setSettlementNo("STL" + String.format("%08d", settlSeq++));
                    settlement.setSettlementType(random.nextBoolean() ? "NORMAL" : "EXPRESS");
                    settlement.setCurrency(currency);
                    settlement.setSettlementAmount(settlAmt);
                    settlement.setPaymentAmount(settlAmt
                            .multiply(BigDecimal.valueOf(0.975))
                            .setScale(2, RoundingMode.HALF_UP));
                    settlement.setSettlementDate(paymentDate.minusDays(1 + random.nextInt(2)));
                    Settlement savedSettl = settlementRepository.save(settlement);
                    settlCount++;
                    totalCAAmount = totalCAAmount.add(settlAmt);

                    for (Transaction tx : sTxns) {
                        tx.setSettlementId(savedSettl.getSettlementId());
                        transactionRepository.save(tx);
                    }
                }

                savedCA.setAmount(totalCAAmount);
                creditAdviceRepository.save(savedCA);
            }
        }
        log.info("Seeded {} credit advices, {} settlements", caCount, settlCount);

        // ── 4. Persist sentinel so we never double-seed ───────────────────────
        Analytics sentinel = new Analytics();
        sentinel.setMerchantId(merchants.get(0).getMerchantId());
        sentinel.setDataName("RECENT_SEED_DONE");
        sentinel.setDataValue("2026-04-26-to-05-06-v3");
        sentinel.setGeneratedAt(LocalDateTime.now());
        analyticsRepository.save(sentinel);

        log.info("=== Recent data seeding complete (Apr 26 – May 6 2026) ===");
    }

    private Refund buildRefund(Transaction tx, String status, int refSeq) {
        Refund r = new Refund();
        r.setTransactionId(tx.getTransactionId());
        r.setMerchantId(tx.getMerchantId());
        r.setCardNo(tx.getCardNo());
        r.setCurrency(tx.getCurrency());
        r.setAmount(tx.getAmount());
        r.setTransactionDate(tx.getTxnDate());
        r.setSubmissionDate(tx.getTxnDate().plusDays(1 + random.nextInt(5)));

        boolean isFull = random.nextBoolean();
        r.setRefundType(isFull ? "FULL" : "PARTIAL");
        if (isFull) {
            r.setRefundAmount(tx.getAmount());
        } else {
            int pct = 20 + random.nextInt(61); // 20–80 %
            r.setRefundAmount(tx.getAmount()
                    .multiply(BigDecimal.valueOf(pct))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        r.setRefundRefNo("RFN" + String.format("%08d", refSeq));
        r.setStatus(status);
        return r;
    }

}
