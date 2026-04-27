package com.a3shield.enforcer.repository;

import com.a3shield.enforcer.permission.Permission;

import java.util.List;

public interface PermissionRepository<U> {
    /**
     *
     * @param userId
     * @return
     */
    List<Permission> getPermissionsBySubjectId(U userId);
}
