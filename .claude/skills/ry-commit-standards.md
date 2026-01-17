# Dragonboat-Backend Commit Standards

Dragonboat-Backend 项目的 Git Commit Message 规范，基于 Conventional Commits 标准。

## 触发词

- "commit 规范"
- "commit message"
- "提交规范"
- "提交信息"
- "提交格式"
- "commit format"
- "how to commit"

## Commit Message 格式

### 基本格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 简化格式（大多数情况）

```
<type>(<scope>): <subject>
```

## Type (类型)

| 类型 | 描述 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(auth): add user login endpoint` |
| `fix` | Bug 修复 | `fix(auth): resolve token expiration issue` |
| `refactor` | 代码重构 | `refactor(cache): simplify cache layer` |
| `docs` | 文档更新 | `docs(readme): update setup instructions` |
| `style` | 代码风格 | `style(format): apply google java format` |
| `test` | 测试相关 | `test(auth): add unit tests for login` |
| `chore` | 构建/工具 | `chore(deps): upgrade spring boot to 3.2` |
| `perf` | 性能优化 | `perf(query): optimize database query` |
| `ci` | CI/CD | `ci(github): add workflow for pr validation` |

## Scope (范围)

对应项目模块或功能区域:

| Scope | 描述 | 示例 |
|-------|------|------|
| `auth` | 认证授权 | `ruoyi-auth` |
| `system` | 系统管理 | `ruoyi-modules/system` |
| `gateway` | 网关 | `ruoyi-gateway` |
| `resource` | 资源服务 | `ruoyi-modules/resource` |
| `workflow` | 工作流 | `ruoyi-modules/workflow` |
| `job` | 定时任务 | `ruoyi-modules/job` |
| `monitor` | 监控 | `ruoyi-modules/monitor` |
| `common` | 公共模块 | `ruoyi-common/*` |
| `api` | API 模块 | `ruoyi-api/*` |
| `infra` | 基础设施 | `ruoyi-infra/*` |
| `build` | 构建配置 | `pom.xml`, `build.gradle` |

## Subject (主题)

- 使用现在时态（"add" 而非 "added"）
- 首字母小写
- 不以句号结尾
- 限制在 50 个字符以内

### 正确示例

```bash
feat(auth): add user login endpoint
fix(gateway): resolve circuit breaker issue
docs(readme): update deployment guide
refactor(cache): improve cache performance
```

### 错误示例

```bash
Added user login endpoint           # ✗ 缺少类型和范围
feat(auth): Added login endpoint.   # ✗ 使用过去时和句号
FEAT(auth): Add Login Endpoint      # ✗ 大写开头
feat(auth):                         # ✗ 缺少主题
feat(auth): add user login endpoint with JWT token validation and refresh mechanism  # ✗ 太长
```

## Body (正文)

### 格式

- 详细描述 "what" 和 "why"，而非 "how"
- 每行限制在 72 个字符以内
- 使用列表时用 `-` 或 `*`

### 示例

```bash
feat(auth): add user login endpoint

- Implement POST /auth/login endpoint
- Add JWT token generation and validation
- Integrate with RuoYi permission system
- Support multiple login methods (password, SMS, email)

Closes DB-101
```

## Footer (脚注)

### 关联 Issue

```
Closes DB-101
Resolves #123
Fixes DB-205
```

### Breaking Changes

```
feat(api): remove deprecated user endpoints

BREAKING CHANGE: The /api/v1/user endpoints have been removed.
Migration guide: /docs/migration-v2.md
```

### 引用 Commit

```
Revert "feat(auth): add oauth login"
This reverts commit abc123f
```

## 完整示例

### 简单 Commit

```bash
fix(auth): resolve token expiration validation

Token validation now checks refresh token expiry before access token.
```

### 复杂 Commit

```bash
feat(auth): implement multi-tenant authentication

- Add tenant ID extraction from request header
- Implement tenant-specific user authentication
- Add tenant isolation in permission checks
- Update token generation to include tenant info

Technical details:
- Tenant ID stored in JWT claim 'tenantId'
- Cache keys prefixed with tenant ID
- Data isolation enforced via MyBatis interceptor

Closes DB-101
Related to DB-102
```

### Breaking Change

```bash
refactor(api): restructure user management endpoints

- Rename /user/list to /api/v1/users
- Change request method from GET to POST for search
- Update response format to include pagination metadata

BREAKING CHANGE: User API endpoints have been restructured.
Old endpoints are deprecated and will be removed in v2.1.0.
Migration guide available at /docs/api-migration.md

Closes DB-301
```

## Commit 最佳实践

### 1. 原子化提交

```bash
# ✓ Good: 每个提交只做一件事
feat(auth): add login endpoint
feat(auth): add logout endpoint
fix(auth): handle token expiry

# ✗ Bad: 一个提交做太多事
feat(auth): add login, logout, and registration endpoints with JWT validation
```

### 2. 清晰的描述

```bash
# ✓ Good: 描述做了什么和为什么
fix(gateway): prevent circuit breaker from opening during startup

The circuit breaker was incorrectly opening during application startup
due to initial connection attempts failing. Added a warmup period to
allow services to initialize properly.

# ✗ Bad: 模糊的描述
fix(gateway): fix circuit breaker
```

### 3. 及时提交

```bash
# ✓ Good: 小而频繁的提交
feat(auth): add login endpoint
feat(auth): add JWT validation
test(auth): add login unit tests
docs(auth): document login flow

# ✗ Bad: 大而罕见的提交
feat(auth): implement complete authentication system with login, logout,
registration, password reset, JWT tokens, unit tests, and documentation
```

## Commit 模板

### 功能开发

```bash
feat(<scope>): <brief description>

- [ ] Main feature 1
- [ ] Main feature 2
- [ ] Main feature 3

Closes DB-XXX
```

### Bug 修复

```bash
fix(<scope>): <brief description>

Describe the bug and the fix.

Root cause: <what was wrong>
Fix: <how it was fixed>

Fixes DB-XXX
```

### 重构

```bash
refactor(<scope>): <brief description>

- Refactored component 1
- Refactored component 2

Reason: <why the refactoring was needed>

No functional changes.
```

## 常用场景

### 1. 添加新功能

```bash
git commit -m "feat(auth): add user login endpoint"
```

### 2. 修复 Bug

```bash
git commit -m "fix(gateway): resolve circuit breaker false positive"
```

### 3. 更新文档

```bash
git commit -m "docs(readme): update setup instructions for Docker"
```

### 4. 添加测试

```bash
git commit -m "test(auth): add unit tests for password validation"
```

### 5. 代码格式化

```bash
git commit -m "style(format): apply google java format"
```

### 6. 更新依赖

```bash
git commit -m "chore(deps): upgrade spring boot to 3.2.0"
```

### 7. 性能优化

```bash
git commit -m "perf(cache): reduce cache query time by 50%"
```

## 验证 Commit 格式

### 自动验证

preToolUse hook 会自动验证 commit 格式:

```bash
# ✓ Valid
git commit -m "feat(auth): add login endpoint"

# ✗ Invalid
git commit -m "add login feature"
# Error: Commit message must follow Conventional Commits format
```

### 手动验证

```bash
# 使用 commitlint 验证
echo "feat(auth): add login" | commitlint

# 查看最近的 commit
git log -3 --pretty=format:"%s"
```

## Commit 命令

### 基本提交

```bash
# 暂存所有更改
git add .

# 提交
git commit -m "feat(auth): add login endpoint"

# 添加文件并提交
git commit -am "fix(auth): handle null token"
```

### 修改最后一次提交

```bash
# 修改提交信息
git commit --amend

# 添加遗漏的文件
git add forgotten-file.txt
git commit --amend --no-edit
```

### 交互式变基

```bash
# 修改最近的 3 次提交
git rebase -i HEAD~3

# 命令:
# pick = 保留
# reword = 修改信息
# edit = 修改提交
# squash = 合并到前一个
# drop = 删除
```

## 与 PR 标题的关系

PR 标题应使用与 commit 相同的格式:

```bash
# PR 标题
feat(auth): implement user authentication system

# 会包含的 commits:
feat(auth): add login endpoint
feat(auth): add JWT validation
test(auth): add authentication tests
```

## 工具集成

### 1. commit-commands skill

使用 `/skill commit-commands:commit` 自动验证和创建 commit

### 2. create-pr skill

使用 `/skill create-pr` 自动生成符合规范的 PR

### 3. GitHub Actions

`.github/workflows/pr-validate.yml` 验证 PR 中的 commit

## 示例项目

查看项目中的 commit 历史:

```bash
# 查看最近的 commits
git log -10 --pretty=format:"%h %s"

# 查看某文件的 commit 历史
git log --oneline -- ruoyi-modules/system/

# 查看某作者的 commits
git log --author="Claude" --oneline
```

## 故障排除

### 问题: Commit 格式验证失败

```bash
# 错误
git commit -m "add new feature"
# Error: Commit message must follow Conventional Commits format

# 正确
git commit -m "feat: add new feature"
```

### 问题: Commit 信息太长

```bash
# 使用 body 添加详细信息
git commit -m "feat(auth): add login endpoint

- Implement POST /auth/login
- Add JWT token generation
- Integrate with permission system"
```

### 问题: 需要修改已推送的 commit

```bash
# 警告: 不要修改已推送的 commit，除非你确定后果
git commit --amend
git push origin feat/DB-101-feature --force-with-lease
```

## 参考资源

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Commitlint](https://commitlint.js.org/)
- [Angular Commit Guidelines](https://github.com/angular/angular/blob/master/CONTRIBUTING.md#commit)

## 配置文件

相关配置:

- `.claude/hooks/preToolUse.cjs` - Commit 格式验证
- `.github/workflows/pr-validate.yml` - PR Commit 验证
- `.commitlintrc.yml` - Commitlint 配置（可选）

## 版本历史

- v1.0.0 (2026-01-17) - 初始版本
