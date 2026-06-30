package com.jumbosoft.erpcustomer.controller;

import com.jumbosoft.erpcustomer.exception.ApiResult;
import com.jumbosoft.erpcustomer.exception.BusinessException;
import com.jumbosoft.erpcustomer.model.dto.request.LoginRequest;
import com.jumbosoft.erpcustomer.model.dto.response.UserResponse;
import com.jumbosoft.erpcustomer.model.entity.User;
import com.jumbosoft.erpcustomer.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", user.getId().toString());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("nickname", user.getNickname());
        return ApiResult.ok(result);
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me(@RequestParam String token) {
        User user = userRepository.findById(Long.parseLong(token))
                .orElseThrow(() -> new BusinessException(401, "未登录"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("nickname", user.getNickname());
        return ApiResult.ok(result);
    }

    @GetMapping("/users")
    public ApiResult<List<UserResponse>> listUsers() {
        return ApiResult.ok(
            userRepository.findAll().stream().map(UserResponse::from).collect(Collectors.toList())
        );
    }

    @PostMapping("/users")
    public ApiResult<UserResponse> addUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String role = body.getOrDefault("role", "VIEWER");
        String nickname = body.getOrDefault("nickname", username);

        if (username == null || username.isEmpty()) throw new BusinessException(400, "用户名不能为空");
        if (password == null || password.isEmpty()) throw new BusinessException(400, "密码不能为空");
        if (userRepository.existsByUsername(username)) throw new BusinessException(400, "用户名已存在");

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setNickname(nickname);
        userRepository.save(user);

        return ApiResult.ok(UserResponse.from(user));
    }

    @PutMapping("/users/{id}")
    public ApiResult<UserResponse> updateUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        if (body.containsKey("password") && !body.get("password").isEmpty()) {
            user.setPassword(passwordEncoder.encode(body.get("password")));
        }
        if (body.containsKey("role")) user.setRole(body.get("role"));
        if (body.containsKey("nickname")) user.setNickname(body.get("nickname"));
        userRepository.save(user);

        return ApiResult.ok(UserResponse.from(user));
    }

    @DeleteMapping("/users/{id}")
    public ApiResult<?> deleteUser(@PathVariable Long id) {
        if (id == 1L) throw new BusinessException(400, "不能删除超级管理员");
        userRepository.deleteById(id);
        return ApiResult.ok();
    }
}
