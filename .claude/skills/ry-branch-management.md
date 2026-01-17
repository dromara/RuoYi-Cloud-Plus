# Dragonboat-Backend Branch Management

Dragonboat-Backend 项目的 Git 分支管理规范和自动化流程。

## 触发词

- "创建分支"
- "new branch"
- "create branch"
- "分支管理"
- "管理分支"
- "切换分支"
- "switch branch"
- "checkout branch"

## 分支策略

项目采用 **GitHub Flow** 简化版分支策略:

```
2.X (protected)
  │
  ├── feat/* (feature branches)
  ├── fix/* (bugfix branches)
  ├── refactor/* (refactoring branches)
  ├── docs/* (documentation branches)
  └── hotfix/* (emergency fixes)
```

## 分支命名规范

### 格式

```
<type>/<ID>-<description>
```

### 类型 (type)

| 类型 | 用途 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat/DB-101-user-auth` |
| `fix` | Bug 修复 | `fix/DB-205-login-error` |
| `refactor` | 代码重构 | `refactor/DB-301-permission` |
| `docs` | 文档更新 | `docs/DB-401-api-docs` |
| `style` | 代码风格 | `style/DB-501-formatting` |
| `test` | 测试相关 | `test/DB-601-unit-tests` |
| `chore` | 构建/工具 | `chore/DB-701-deps-update` |
| `hotfix` | 紧急修复 | `hotfix/DB-801-security` |

### ID (Issue ID)

使用项目 Issue 跟踪系统的编号:

- **DB-***: Dragonboat-Backend 内部 Issue
- **#***: GitHub Issue 编号

### Description (描述)

简短的英文描述，使用 kebab-case:

```
feat/DB-101-user-auth         ✓ Good
feat/DB-101-userAuthentication ✗ Bad (use kebab-case)
feat/DB-101                   ✗ Bad (missing description)
feature-branch                ✗ Bad (wrong format)
```

## 保护分支

以下分支受保护，需要 PR 才能合并:

| 分支 | 保护规则 | 审批要求 |
|------|----------|----------|
| `2.X` | 禁止直接推送 | 需要 1 个审批 |
| `main` | 禁止直接推送 | 需要 1 个审批 |

## 工作流程

### 1. 开始新功能

```bash
# 1. 确保在 2.X 分支且是最新的
git checkout 2.X
git pull origin 2.X

# 2. 创建新分支
git checkout -b feat/DB-101-user-auth

# 3. 推送到远程
git push -u origin feat/DB-101-user-auth
```

### 2. 开发阶段

```bash
# 在分支上开发
# ... 编写代码 ...

# 查看状态
git status

# 暂存文件
git add .

# 提交 (遵循 commit 规范)
git commit -m "feat(auth): add user login endpoint"
```

### 3. 完成 PR

```bash
# 推送更改
git push origin feat/DB-101-user-auth

# 创建 PR (使用 gh CLI 或 /skill create-pr)
gh pr create --base 2.X --head feat/DB-101-user-auth
```

### 4. 合并后清理

```bash
# 切换回 2.X
git checkout 2.X
git pull origin 2.X

# 删除本地分支
git branch -d feat/DB-101-user-auth

# 删除远程分支
git push origin --delete feat/DB-101-user-auth
```

## 常用命令

### 查看分支

```bash
# 查看所有分支
git branch -a

# 查看远程分支
git branch -r

# 查看当前分支
git branch --show-current

# 查看分支详情
git branch -vv
```

### 创建分支

```bash
# 从 2.X 创建新分支
git checkout 2.X
git checkout -b feat/DB-101-user-auth

# 从当前提交创建新分支
git checkout -b feat/DB-102-feature-name

# 从指定 commit 创建分支
git checkout -b fix/DB-201-bug-fix abc123f
```

### 切换分支

```bash
# 切换到已有分支
git checkout feat/DB-101-user-auth

# 切换到上一个分支
git checkout -

# 创建并切换到新分支
git checkout -b feat/DB-103-new-feature
```

### 删除分支

```bash
# 删除本地分支 (已合并)
git branch -d feat/DB-101-user-auth

# 强制删除本地分支
git branch -D feat/DB-101-user-auth

# 删除远程分支
git push origin --delete feat/DB-101-user-auth

# 清理已删除的远程分支
git remote prune origin
```

### 重命名分支

```bash
# 重命名当前分支
git branch -m feat/DB-101-user-auth-new

# 重命名指定分支
git branch -m feat/DB-101-old feat/DB-101-new
```

## 分支命名验证

### 正确示例

```bash
feat/DB-101-user-auth           ✓
fix/DB-205-login-error          ✓
refactor/DB-301-cache-layer     ✓
docs/DB-401-readme-update       ✓
hotfix/DB-801-security-patch    ✓
```

### 错误示例

```bash
feature-branch                  ✗ 缺少类型和 ID
feat-user-auth                  ✗ 缺少 ID
feat/DB-101_userAuthentication  ✗ 使用 camelCase
feat/DB-101                     ✗ 缺少描述
feat/DB-101-用户认证            ✗ 使用中文描述
feat/db-101-user-auth           ✗ ID 使用小写
feat/GH-101-user-auth           ✓ 可以使用 GitHub Issue
```

## 自动化规则

### preToolUse Hook 验证

当尝试创建分支时，自动验证命名:

```javascript
// 如果分支命名不符合规范，阻止操作
if (!branchName.match(/^(feat|fix|refactor|docs|style|test|chore|hotfix)\/[A-Z]+-\d+-[a-z0-9-]+$/)) {
  return { valid: false, errorMessage: "分支命名不符合规范" };
}
```

### 分支保护

阻止直接提交到保护分支:

```bash
# ❌ 这将被阻止
git checkout 2.X
git commit -m "some change"

# ✅ 正确做法
git checkout -b feat/DB-101-some-change
git commit -m "feat: some change"
```

## 分支状态检查

### 检查分支是否落后

```bash
# 检查当前分支相对于 2.X 的状态
git log 2.X..HEAD

# 检查是否有未推送的提交
git log origin/feat/DB-101-user-auth..HEAD

# 检查是否需要合并 2.X 的最新更改
git fetch origin
git log HEAD..origin/2.X
```

### 检查分支差异

```bash
# 查看与 2.X 的文件差异
git diff 2.X...HEAD

# 查看统计信息
git diff --stat 2.X...HEAD

# 查看提交历史
git log --oneline 2.X..HEAD
```

## 分支策略最佳实践

### 1. 小而频繁的 PR

```
✓ Good: 每个分支 1-3 天，1-5 个提交
✗ Bad: 每个分支 2 周，50+ 个提交
```

### 2. 及时同步 2.X

```bash
# 每天/每次开始工作前
git checkout 2.X
git pull origin 2.X
git checkout feat/DB-101-user-auth
git merge 2.X
```

### 3. 清晰的提交历史

```bash
# ✓ Good: 清晰的提交信息
feat(auth): add login endpoint
feat(auth): add JWT validation
fix(auth): handle token expiry

# ✗ Bad: 模糊的提交信息
update
fix bug
some changes
```

### 4. 分支命名一致性

在团队中使用一致的命名规范:

```
feat/DB-101-user-auth
feat/DB-102-user-profile
feat/DB-103-user-permissions

# 而不是
feat/DB-101-user-auth
feature/user-profile
user-permissions
```

## 故障排除

### 问题: 分支命名不符合规范

```bash
# 错误
git checkout -b feature-branch
# Branch naming validation failed: 分支命名不符合规范

# 正确
git checkout -b feat/DB-101-user-auth
```

### 问题: 无法推送保护分支

```bash
# 错误
git push origin 2.X
# ! [rejected] 2.X -> 2.X (branch is currently protected)

# 正确
git checkout -b feat/DB-101-feature
git push -u origin feat/DB-101-feature
# 然后创建 PR
```

### 问题: 分支落后于 2.X

```bash
# 合并 2.X 的最新更改
git fetch origin
git merge origin/2.X

# 如果有冲突，解决后
git add .
git commit -m "chore: merge 2.X changes"
```

## 与其他技能集成

- **ry-workflow**: 主工作流协调器调用分支管理
- **ry-commit-standards**: 提交规范配合分支策略
- **ry-code-review**: 审查时检查分支命名

## 配置文件

相关配置:

- `.claude/hooks/preToolUse.cjs` - 分支命名验证
- `.github/workflows/pr-validate.yml` - PR 时的分支验证

## 版本历史

- v1.0.0 (2026-01-17) - 初始版本
