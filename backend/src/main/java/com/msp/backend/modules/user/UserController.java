package com.msp.backend.modules.user;

import com.msp.backend.modules.role.Permission;
import com.msp.backend.modules.role.PermissionRepository;
import com.msp.backend.modules.role.RolePermissionRepository;
import com.msp.backend.modules.role.RoleRepository;
import com.msp.backend.modules.merchant.MerchantUserMapping;
import com.msp.backend.modules.merchant.MerchantUserMappingRepository;
import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.util.AuditHelper;
import com.msp.backend.util.MerchantResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor

public class UserController {

    private final UserService userService;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final MerchantUserMappingRepository merchantUserMappingRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantResolver merchantResolver;

    // GET http://localhost:8001/api/users
    @GetMapping
    public List<Map<String, Object>> getAllUsers() {
        // If requester is a merchant user, only return users linked to the same merchant(s)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String requesterEmail = auth.getName();
        User requester = userRepository.findByEmailAndDeletedAtIsNull(requesterEmail).orElse(null);
        java.util.Set<Long> allowedUserIds = null;
        boolean requesterIsAdmin = false;
        boolean hasManageUsers = false;
        boolean hasManageChildUsers = false;
        if (requester != null) {
            userService.populateRole(requester);
            requesterIsAdmin = "ADMIN".equals(requester.getRole());
            // Resolve requester permissions
            List<UserRole> reqRoles = userRoleRepository.findByUserId(requester.getUserId());
            java.util.Set<String> reqPerms = new java.util.HashSet<>();
            for (UserRole ur : reqRoles) {
                rolePermissionRepository.findByRoleId(ur.getRoleId()).forEach(rp ->
                    permissionRepository.findById(rp.getPermissionId()).ifPresent(p -> reqPerms.add(p.getPermissionName())));
            }
            hasManageUsers = reqPerms.contains("MANAGE_USERS");
            hasManageChildUsers = reqPerms.contains("MANAGE_CHILD_USERS");
            if (!requesterIsAdmin) {
                List<Long> myMerchantIds = merchantResolver.resolveAllForUser(requester);
                allowedUserIds = new java.util.HashSet<>();
                for (Long mid : myMerchantIds) {
                    List<MerchantUserMapping> mappings = merchantUserMappingRepository.findByMerchantId(mid);
                    for (MerchantUserMapping mum : mappings) {
                        allowedUserIds.add(mum.getUserId());
                    }
                }
            }
        }
        List<User> users = userService.getAllUsers();
        final java.util.Set<Long> finalAllowed = allowedUserIds;
        if (finalAllowed != null) {
            users = users.stream().filter(u -> finalAllowed.contains(u.getUserId())).collect(Collectors.toList());
        }
        // If requester only has MANAGE_CHILD_USERS (not MANAGE_USERS and not admin),
        // filter to only show self + all descendants (children, grandchildren, etc.)
        final boolean onlyChildAccess = !requesterIsAdmin && !hasManageUsers && hasManageChildUsers;
        final String requesterEmailFinal = requesterEmail;
        if (onlyChildAccess && requester != null) {
            final Long requesterId = requester.getUserId();
            // Build set of all descendant emails (recursive)
            java.util.Set<String> descendantEmails = collectAllDescendantEmails(requesterEmailFinal, users);
            users = users.stream().filter(u ->
                u.getUserId().equals(requesterId) || descendantEmails.contains(u.getEmail())
            ).collect(Collectors.toList());
        }
        final boolean finalIsAdmin = requesterIsAdmin;
        final boolean finalHasManageUsers = hasManageUsers;
        final String finalRequesterEmail2 = requesterEmail;
        return users.stream().map(u -> {
            // Determine userType: ADMIN if user has ANY role with roleType=SYSTEM, else MERCHANT
            boolean isAdmin = userRoleRepository.findByUserId(u.getUserId()).stream()
                    .anyMatch(ur -> roleRepository.findById(ur.getRoleId())
                            .map(r -> "SYSTEM".equalsIgnoreCase(r.getRoleType())).orElse(false));
            // Resolve linked merchant names
            List<MerchantUserMapping> userMappings = merchantUserMappingRepository.findByUserId(u.getUserId());
            List<String> merchantNames = userMappings.stream()
                    .map(mum -> merchantRepository.findById(mum.getMerchantId()).map(mer -> mer.getMerchantName()).orElse("#" + mum.getMerchantId()))
                    .collect(Collectors.toList());
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("userId", u.getUserId());
            m.put("email", u.getEmail());
            m.put("firstName", u.getFirstName());
            m.put("lastName", u.getLastName());
            m.put("displayName", u.getDisplayName());
            m.put("contactNumber", u.getContactNumber());
            m.put("status", u.getStatus());
            m.put("role", u.getRole());
            m.put("userType", isAdmin ? "ADMIN" : "MERCHANT");
            m.put("mfaEnabled", u.isMfaEnabled());
            m.put("merchantNames", merchantNames);
            m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            m.put("lastLoginAt", u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : null);
            m.put("createdBy", u.getCreatedBy() != null ? u.getCreatedBy() : "");
            // canManage: true if requester is admin, has MANAGE_USERS, or user is a descendant
            boolean canManage = finalIsAdmin || finalHasManageUsers || isDescendantUser(u.getUserId(), finalRequesterEmail2);
            // Never allow edit/delete on own account
            if (u.getEmail() != null && u.getEmail().equals(finalRequesterEmail2)) canManage = false;
            // Never allow edit/delete on the default System Admin (super admin)
            if ("ADMIN".equals(u.getRole()) && (u.getCreatedBy() == null || u.getCreatedBy().isBlank())) canManage = false;
            m.put("canManage", canManage);
            return m;
        }).toList();
    }

    // Search users by name or email (for merchant user mapping)
    @GetMapping("/search")
    public List<java.util.Map<String, Object>> searchUsers(@RequestParam String q) {
        String keyword = q.toLowerCase().trim();
        return userRepository.findByDeletedAtIsNull().stream()
            .filter(u -> {
                String full = ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                        + (u.getLastName() != null ? u.getLastName() : "") + " "
                        + (u.getEmail() != null ? u.getEmail() : "")).toLowerCase();
                return full.contains(keyword);
            })
            .limit(10)
            .map(u -> {
                userService.populateRole(u);
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("userId", u.getUserId());
                m.put("email", u.getEmail());
                m.put("firstName", u.getFirstName());
                m.put("lastName", u.getLastName());
                m.put("displayName", u.getDisplayName());
                m.put("status", u.getStatus());
                m.put("role", u.getRole());
                return m;
            })
            .toList();
    }

    // GET single user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    // Check if a display name is already taken (used for inline validation)
    @GetMapping("/check-display-name")
    public ResponseEntity<Map<String, Object>> checkDisplayName(
            @RequestParam String name,
            @RequestParam(required = false) Long excludeId) {
        boolean taken;
        if (excludeId != null) {
            taken = userRepository.existsByDisplayNameIgnoreCaseAndUserIdNotAndDeletedAtIsNull(name.trim(), excludeId);
        } else {
            taken = userRepository.existsByDisplayNameIgnoreCaseAndDeletedAtIsNull(name.trim());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("taken", taken);
        return ResponseEntity.ok(result);
    }

    // GET user with role + permissions details
    @GetMapping("/{id}/details")
    public ResponseEntity<Map<String, Object>> getUserDetails(@PathVariable Long id) {
        enforceChildUserAccess(id);
        User user = userService.getUserById(id);
        user.setPassword(null);

        // Get role info
        List<UserRole> userRoles = userRoleRepository.findByUserId(id);

        // Build roles list with full role objects
        List<Map<String, Object>> rolesList = new ArrayList<>();
        List<Permission> permissions = List.of();
        for (UserRole ur : userRoles) {
            roleRepository.findById(ur.getRoleId()).ifPresent(role -> {
                Map<String, Object> rm = new HashMap<>();
                rm.put("roleId", role.getRoleId());
                rm.put("roleName", role.getRoleName());
                rm.put("roleType", role.getRoleType() != null ? role.getRoleType() : "");
                rm.put("description", role.getDescription() != null ? role.getDescription() : "");
                rolesList.add(rm);
            });
        }

        // Get permissions via first role
        if (!userRoles.isEmpty()) {
            Long roleId = userRoles.get(0).getRoleId();
            List<Long> permIds = rolePermissionRepository.findByRoleId(roleId)
                .stream().map(rp -> rp.getPermissionId()).collect(Collectors.toList());
            if (!permIds.isEmpty()) {
                permissions = permissionRepository.findAllById(permIds);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        result.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        result.put("email", user.getEmail());
        result.put("contactNumber", user.getContactNumber() != null ? user.getContactNumber() : "");
        result.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "");
        result.put("status", user.getStatus() != null ? user.getStatus() : "");
        result.put("role", user.getRole() != null ? user.getRole() : "");
        result.put("mfaEnabled", user.isMfaEnabled());
        result.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        result.put("createdBy", AuditHelper.resolveDisplayName(user.getCreatedBy(), userRepository));
        result.put("lastModifiedAt", user.getLastModifiedAt() != null ? user.getLastModifiedAt().toString() : "");
        result.put("lastModifiedBy", AuditHelper.resolveDisplayName(user.getLastModifiedBy(), userRepository));
        result.put("roles", rolesList);
        result.put("permissions", permissions);
        // Linked merchants
        List<MerchantUserMapping> userMappings = merchantUserMappingRepository.findByUserId(id);
        List<Map<String, Object>> linkedMerchants = new ArrayList<>();
        for (MerchantUserMapping mum : userMappings) {
            merchantRepository.findById(mum.getMerchantId()).ifPresent(m -> {
                Map<String, Object> mm = new HashMap<>();
                mm.put("merchantId", m.getMerchantId());
                mm.put("merchantName", m.getMerchantName());
                mm.put("status", m.getStatus());
                linkedMerchants.add(mm);
            });
        }
        result.put("linkedMerchants", linkedMerchants);
        return ResponseEntity.ok(result);
    }

    // 1. ADD ENDPOINT
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        // If the requester is a merchant user, enforce restrictions:
        // - Can only create MERCHANT-type users (not ADMIN)
        // - New user is auto-linked to the same merchants as the creator
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String requesterEmail = auth.getName();
            userRepository.findByEmailAndDeletedAtIsNull(requesterEmail).ifPresent(requester -> {
                userService.populateRole(requester);
                if (!"ADMIN".equals(requester.getRole())) {
                    // Merchant user: only MERCHANT-type users allowed
                    String roleToCreate = user.getRole();
                    if (roleToCreate != null && roleToCreate.equalsIgnoreCase("ADMIN")) {
                        throw new RuntimeException("Merchant users cannot create Admin accounts.");
                    }
                    // Force MERCHANT role if not set
                    if (roleToCreate == null || roleToCreate.isBlank()) {
                        user.setRole("MERCHANT");
                    }
                }
            });
        }
        User created = userService.createUser(user);
        return ResponseEntity.ok(created);
    }

    /** Returns the list of merchants the currently-logged-in user is linked to.
     *  Merchant users call this to know which merchant(s) they belong to. */
    @GetMapping("/my-merchants")
    public ResponseEntity<List<Map<String, Object>>> getMyMerchants() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User me = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
        List<MerchantUserMapping> mappings = merchantUserMappingRepository.findByUserId(me.getUserId());
        List<Map<String, Object>> result = mappings.stream().map(m -> {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("merchantId", m.getMerchantId());
            merchantRepository.findById(m.getMerchantId()).ifPresent(merchant -> {
                row.put("merchantName", merchant.getMerchantName());
            });
            return row;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // UPDATE user
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        enforceChildUserAccess(id);
        User user = new User();
        user.setFirstName(body.get("firstName"));
        user.setLastName(body.get("lastName"));
        user.setDisplayName(body.get("displayName"));
        user.setContactNumber(body.get("contactNumber"));
        user.setStatus(body.get("status"));
        // password is intentionally never set here
        User updated = userService.updateUser(id, user);
        return ResponseEntity.ok(updated);
    }

    // 2. DELETE ENDPOINT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        enforceChildUserAccess(id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Sync all assigned roles for a user (replaces existing set with exactly the provided roles)
    @Transactional
    @PutMapping("/{id}/roles")
    public ResponseEntity<Void> syncRoles(@PathVariable Long id, @RequestBody java.util.List<Long> roleIds) {
        String actor = AuditHelper.currentUser();

        // Delete all current roles then re-insert exactly what was passed
        userRoleRepository.deleteByUserId(id);

        java.util.Set<Long> inserted = new java.util.HashSet<>();
        for (Long roleId : roleIds) {
            if (roleId != null && !inserted.contains(roleId)) {
                UserRole ur = new UserRole();
                ur.setUserId(id);
                ur.setRoleId(roleId);
                ur.setCreatedBy(actor);
                ur.setLastModifiedBy(actor);
                ur.setLastModifiedAt(java.time.LocalDateTime.now());
                userRoleRepository.save(ur);
                inserted.add(roleId);
            }
        }
        return ResponseEntity.ok().build();
    }

    // Sync all linked merchants for a user (replaces existing set)
    @Transactional
    @PutMapping("/{id}/merchants")
    public ResponseEntity<Void> syncMerchants(@PathVariable Long id, @RequestBody List<Long> merchantIds) {
        String actor = AuditHelper.currentUser();
        // Remove existing mappings
        merchantUserMappingRepository.deleteByUserId(id);
        // Insert new mappings
        java.util.Set<Long> inserted = new java.util.HashSet<>();
        for (Long merchantId : merchantIds) {
            if (merchantId != null && !inserted.contains(merchantId)) {
                MerchantUserMapping mapping = new MerchantUserMapping();
                mapping.setMerchantId(merchantId);
                mapping.setUserId(id);
                mapping.setCreatedBy(actor);
                mapping.setLastModifiedBy(actor);
                merchantUserMappingRepository.save(mapping);
                inserted.add(merchantId);
            }
        }
        return ResponseEntity.ok().build();
    }

    // Unassign a single role from user
    @DeleteMapping("/{id}/roles/{roleId}")
    public ResponseEntity<Void> unassignRole(@PathVariable Long id, @PathVariable Long roleId) {
        userRoleRepository.findByUserId(id).stream()
            .filter(ur -> ur.getRoleId().equals(roleId))
            .forEach(ur -> userRoleRepository.delete(ur));
        return ResponseEntity.ok().build();
    }

    // Reset user password (admin action)
    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        enforceChildUserAccess(id);
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        userService.resetPassword(id, newPassword);
        return ResponseEntity.ok().build();
    }

    /**
     * Enforces MANAGE_CHILD_USERS restriction:
     * If the requester is NOT admin and does NOT have MANAGE_USERS but HAS MANAGE_CHILD_USERS,
     * they can only access users they created (createdBy == requester email).
     * Admins and MANAGE_USERS holders bypass this check.
     */
    private void enforceChildUserAccess(Long targetUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return;
        String requesterEmail = auth.getName();
        User requester = userRepository.findByEmailAndDeletedAtIsNull(requesterEmail).orElse(null);
        if (requester == null) return;
        userService.populateRole(requester);
        if ("ADMIN".equals(requester.getRole())) return;
        // Resolve permissions
        java.util.Set<String> perms = new java.util.HashSet<>();
        for (UserRole ur : userRoleRepository.findByUserId(requester.getUserId())) {
            rolePermissionRepository.findByRoleId(ur.getRoleId()).forEach(rp ->
                permissionRepository.findById(rp.getPermissionId()).ifPresent(p -> perms.add(p.getPermissionName())));
        }
        if (perms.contains("MANAGE_USERS")) return;
        if (perms.contains("MANAGE_CHILD_USERS")) {
            if (requester.getUserId().equals(targetUserId)) return;
            // Check if target is a descendant (child, grandchild, etc.)
            if (isDescendantUser(targetUserId, requesterEmail)) return;
            throw new org.springframework.security.access.AccessDeniedException(
                "You can only manage users you created.");
        }
    }

    /** Recursively collect all descendant emails from a given ancestor email */
    private java.util.Set<String> collectAllDescendantEmails(String ancestorEmail, List<User> allUsers) {
        java.util.Set<String> descendants = new java.util.HashSet<>();
        java.util.Queue<String> queue = new java.util.LinkedList<>();
        queue.add(ancestorEmail);
        while (!queue.isEmpty()) {
            String parentEmail = queue.poll();
            for (User u : allUsers) {
                if (parentEmail.equals(u.getCreatedBy()) && !descendants.contains(u.getEmail())) {
                    descendants.add(u.getEmail());
                    queue.add(u.getEmail());
                }
            }
        }
        return descendants;
    }

    /** Check if targetUserId is a descendant of ancestorEmail (recursive) */
    private boolean isDescendantUser(Long targetUserId, String ancestorEmail) {
        User target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) return false;
        java.util.Set<Long> visited = new java.util.HashSet<>();
        return isDescendantUserRecursive(target, ancestorEmail, visited);
    }

    private boolean isDescendantUserRecursive(User user, String ancestorEmail, java.util.Set<Long> visited) {
        if (visited.contains(user.getUserId())) return false;
        visited.add(user.getUserId());
        if (ancestorEmail.equals(user.getCreatedBy())) return true;
        if (user.getCreatedBy() == null || user.getCreatedBy().isBlank()) return false;
        return userRepository.findByEmailAndDeletedAtIsNull(user.getCreatedBy())
                .map(parent -> isDescendantUserRecursive(parent, ancestorEmail, visited))
                .orElse(false);
    }
}