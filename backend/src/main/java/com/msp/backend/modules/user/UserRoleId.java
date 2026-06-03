package com.msp.backend.modules.user;

import java.io.Serializable;
import lombok.Data;

@Data
public class UserRoleId implements Serializable {
    private Long userId;
    private Long roleId;
}
