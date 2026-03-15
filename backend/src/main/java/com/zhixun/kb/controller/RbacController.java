package com.zhixun.kb.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.kb.common.ApiResponse;
import com.zhixun.kb.dto.MenuSaveRequest;
import com.zhixun.kb.dto.RoleSaveRequest;
import com.zhixun.kb.dto.UserSaveRequest;
import com.zhixun.kb.dto.UserUpdateRequest;
import com.zhixun.kb.entity.SysMenu;
import com.zhixun.kb.entity.SysRole;
import com.zhixun.kb.entity.SysRoleMenu;
import com.zhixun.kb.entity.SysUser;
import com.zhixun.kb.entity.SysUserRole;
import com.zhixun.kb.mapper.SysMenuMapper;
import com.zhixun.kb.mapper.SysRoleMapper;
import com.zhixun.kb.mapper.SysRoleMenuMapper;
import com.zhixun.kb.mapper.SysUserMapper;
import com.zhixun.kb.mapper.SysUserRoleMapper;
import com.zhixun.kb.service.SysMenuService;
import com.zhixun.kb.service.SysRoleService;
import com.zhixun.kb.service.SysUserService;
import com.zhixun.kb.vo.SysRoleVO;
import com.zhixun.kb.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class RbacController {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysMenuService sysMenuService;

    @GetMapping("/users")
    @SaCheckPermission("system:user:list")
    public ApiResponse<List<SysUserVO>> listUsers() {
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getId));
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, List<Long>> userRoleIds = userRoles.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId,
                        Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())));
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).distinct().toList();
        Map<Long, String> roleNameMap = roleIds.isEmpty()
                ? new HashMap<>()
                : sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds)).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName));

        List<SysUserVO> result = users.stream().map(u -> {
            List<Long> roleIdList = userRoleIds.getOrDefault(u.getId(), Collections.emptyList());
            List<String> roleNames = roleIdList.stream()
                    .map(roleNameMap::get)
                    .filter(Objects::nonNull)
                    .toList();
            SysUserVO vo = new SysUserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setStatus(u.getStatus());
            vo.setCreatedAt(u.getCreatedAt());
            vo.setRoleIds(roleIdList);
            vo.setRoleNames(roleNames);
            return vo;
        }).toList();
        return ApiResponse.ok(result);
    }

    @PostMapping("/users")
    @SaCheckPermission("system:user:add")
    public ApiResponse<String> createUser(@RequestBody UserSaveRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            return ApiResponse.fail("用户名和密码不能为空");
        }
        Long exists = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (exists > 0) return ApiResponse.fail("用户名已存在");

        SysUser user = new SysUser();
        user.setUsername(request.getUsername().trim());
        user.setPassword(request.getPassword().trim());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setRole("USER");
        sysUserMapper.insert(user);
        sysUserService.updateUserRoles(user.getId(), request.getRoleIds());
        return ApiResponse.ok("created");
    }

    @PutMapping("/users/{id}")
    @SaCheckPermission("system:user:edit")
    public ApiResponse<String> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) return ApiResponse.fail("用户不存在");
        if (StringUtils.hasText(request.getUsername())) {
            Long exists = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, request.getUsername().trim())
                    .ne(SysUser::getId, id));
            if (exists > 0) return ApiResponse.fail("用户名已存在");
            user.setUsername(request.getUsername().trim());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(request.getPassword().trim());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        sysUserMapper.updateById(user);
        if (request.getRoleIds() != null) {
            sysUserService.updateUserRoles(id, request.getRoleIds());
        }
        return ApiResponse.ok("updated");
    }

    @DeleteMapping("/users/{id}")
    @SaCheckPermission("system:user:delete")
    public ApiResponse<String> deleteUser(@PathVariable Long id) {
        if (id == 1L) return ApiResponse.fail("默认管理员不可删除");
        if (Objects.equals(StpUtil.getLoginIdAsLong(), id)) return ApiResponse.fail("不能删除当前登录用户");
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        sysUserMapper.deleteById(id);
        return ApiResponse.ok("deleted");
    }

    @PostMapping("/users/{id}/roles")
    @SaCheckPermission("system:user:grant")
    public ApiResponse<String> updateUserRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) return ApiResponse.fail("用户不存在");
        sysUserService.updateUserRoles(id, roleIds);
        return ApiResponse.ok("updated");
    }

    @GetMapping("/roles")
    @SaCheckPermission("system:role:list")
    public ApiResponse<List<SysRoleVO>> listRoles() {
        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, List<Long>> roleMenuIds = roleMenus.stream()
                .collect(Collectors.groupingBy(SysRoleMenu::getRoleId,
                        Collectors.mapping(SysRoleMenu::getMenuId, Collectors.toList())));

        List<SysRoleVO> result = roles.stream().map(r -> {
            SysRoleVO vo = new SysRoleVO();
            vo.setId(r.getId());
            vo.setRoleName(r.getRoleName());
            vo.setRoleKey(r.getRoleKey());
            vo.setStatus(r.getStatus());
            vo.setCreateTime(r.getCreateTime());
            vo.setUpdateTime(r.getUpdateTime());
            vo.setMenuIds(roleMenuIds.getOrDefault(r.getId(), Collections.emptyList()));
            return vo;
        }).toList();
        return ApiResponse.ok(result);
    }

    @PostMapping("/roles")
    @SaCheckPermission("system:role:add")
    public ApiResponse<String> createRole(@RequestBody RoleSaveRequest request) {
        if (!StringUtils.hasText(request.getRoleName()) || !StringUtils.hasText(request.getRoleKey())) {
            return ApiResponse.fail("角色名称和标识不能为空");
        }
        Long exists = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, request.getRoleKey().trim()));
        if (exists > 0) return ApiResponse.fail("角色标识已存在");

        SysRole role = new SysRole();
        role.setRoleName(request.getRoleName().trim());
        role.setRoleKey(request.getRoleKey().trim());
        role.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "0");
        sysRoleMapper.insert(role);
        sysRoleService.updateRoleMenus(role.getId(), request.getMenuIds());
        return ApiResponse.ok("created");
    }

    @PutMapping("/roles/{id}")
    @SaCheckPermission("system:role:edit")
    public ApiResponse<String> updateRole(@PathVariable Long id, @RequestBody RoleSaveRequest request) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) return ApiResponse.fail("角色不存在");
        if (StringUtils.hasText(request.getRoleName())) role.setRoleName(request.getRoleName().trim());
        if (StringUtils.hasText(request.getRoleKey())) {
            Long exists = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getRoleKey, request.getRoleKey().trim())
                    .ne(SysRole::getId, id));
            if (exists > 0) return ApiResponse.fail("角色标识已存在");
            role.setRoleKey(request.getRoleKey().trim());
        }
        if (StringUtils.hasText(request.getStatus())) role.setStatus(request.getStatus());
        sysRoleMapper.updateById(role);
        if (request.getMenuIds() != null) {
            sysRoleService.updateRoleMenus(id, request.getMenuIds());
        }
        return ApiResponse.ok("updated");
    }

    @DeleteMapping("/roles/{id}")
    @SaCheckPermission("system:role:delete")
    public ApiResponse<String> deleteRole(@PathVariable Long id) {
        if (id == 1L) return ApiResponse.fail("默认管理员角色不可删除");
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        sysRoleMapper.deleteById(id);
        return ApiResponse.ok("deleted");
    }

    @PostMapping("/roles/{id}/menus")
    @SaCheckPermission("system:role:grant")
    public ApiResponse<String> updateRoleMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) return ApiResponse.fail("角色不存在");
        sysRoleService.updateRoleMenus(id, menuIds);
        return ApiResponse.ok("updated");
    }

    @GetMapping("/menus")
    @SaCheckPermission("system:menu:list")
    public ApiResponse<List<SysMenu>> listMenus(@RequestParam(defaultValue = "tree") String mode) {
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId));
        if ("flat".equalsIgnoreCase(mode)) {
            return ApiResponse.ok(menus);
        }
        return ApiResponse.ok(sysMenuService.buildMenuTree(menus));
    }

    @GetMapping("/menus/my")
    public ApiResponse<List<SysMenu>> listMyMenus() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.ok(sysMenuService.selectMenuTreeByUserId(userId));
    }

    @GetMapping("/perms/my")
    public ApiResponse<List<String>> listMyPerms() {
        return ApiResponse.ok(StpUtil.getPermissionList());
    }

    @PostMapping("/menus")
    @SaCheckPermission("system:menu:add")
    public ApiResponse<String> createMenu(@RequestBody MenuSaveRequest request) {
        if (!StringUtils.hasText(request.getMenuName())) return ApiResponse.fail("菜单名称不能为空");
        SysMenu menu = new SysMenu();
        menu.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        menu.setMenuName(request.getMenuName().trim());
        menu.setPath(request.getPath());
        menu.setComponent(request.getComponent());
        menu.setPerms(request.getPerms());
        menu.setIcon(request.getIcon());
        menu.setMenuType(StringUtils.hasText(request.getMenuType()) ? request.getMenuType() : "C");
        menu.setSort(request.getSort() == null ? 0 : request.getSort());
        menu.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "0");
        sysMenuMapper.insert(menu);
        return ApiResponse.ok("created");
    }

    @PutMapping("/menus/{id}")
    @SaCheckPermission("system:menu:edit")
    public ApiResponse<String> updateMenu(@PathVariable Long id, @RequestBody MenuSaveRequest request) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) return ApiResponse.fail("菜单不存在");
        if (request.getParentId() != null) menu.setParentId(request.getParentId());
        if (StringUtils.hasText(request.getMenuName())) menu.setMenuName(request.getMenuName().trim());
        if (request.getPath() != null) menu.setPath(request.getPath());
        if (request.getComponent() != null) menu.setComponent(request.getComponent());
        if (request.getPerms() != null) menu.setPerms(request.getPerms());
        if (request.getIcon() != null) menu.setIcon(request.getIcon());
        if (StringUtils.hasText(request.getMenuType())) menu.setMenuType(request.getMenuType());
        if (request.getSort() != null) menu.setSort(request.getSort());
        if (StringUtils.hasText(request.getStatus())) menu.setStatus(request.getStatus());
        sysMenuMapper.updateById(menu);
        return ApiResponse.ok("updated");
    }

    @DeleteMapping("/menus/{id}")
    @SaCheckPermission("system:menu:delete")
    public ApiResponse<String> deleteMenu(@PathVariable Long id) {
        List<SysMenu> allMenus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>());
        Set<Long> ids = collectMenuAndChildren(id, allMenus);
        if (ids.isEmpty()) return ApiResponse.fail("菜单不存在");
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getMenuId, ids));
        sysMenuMapper.delete(new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, ids));
        return ApiResponse.ok("deleted");
    }

    private Set<Long> collectMenuAndChildren(Long rootId, List<SysMenu> allMenus) {
        Map<Long, List<SysMenu>> childMap = allMenus.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        Map<Long, SysMenu> menuMap = allMenus.stream().collect(Collectors.toMap(SysMenu::getId, Function.identity()));
        if (!menuMap.containsKey(rootId)) {
            return Collections.emptySet();
        }
        Set<Long> ids = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            if (!ids.add(id)) continue;
            List<SysMenu> children = childMap.getOrDefault(id, Collections.emptyList());
            for (SysMenu child : children) {
                queue.add(child.getId());
            }
        }
        return ids;
    }
}
