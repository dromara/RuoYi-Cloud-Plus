/**
 * Dragonboat-Backend preToolUse Hook
 *
 * Validates Git operations before execution to enforce workflow standards.
 *
 * Features:
 * - Block direct commits to protected branches (2.X, main)
 * - Validate branch naming conventions
 * - Validate commit message format
 * - Prevent force pushes to protected branches
 */

const PROTECTED_BRANCHES = ['2.X', 'main'];
const BRANCH_NAME_PATTERN = /^(feat|fix|refactor|docs|style|test|chore|perf|ci|hotfix)\/([A-Z]+-\d+|[A-Z]+\d+)-([a-z0-9-]+)$/;
const COMMIT_PATTERN = /^(feat|fix|refactor|docs|style|test|chore|perf|ci|build|revert)(\(.+\))?!?:\s.+$/;

/**
 * Get current branch name
 */
function getCurrentBranch(command) {
  // Try to get current branch from git command
  if (command.includes('git branch --show-current')) {
    return command.split(' ').pop();
  }

  // For other commands, try to extract branch from args
  const branchMatch = command.match(/(?:-b\s+|--create-branch=|HEAD\s+)([\w\-/]+)/);
  if (branchMatch) {
    return branchMatch[1];
  }

  return null;
}

/**
 * Check if a branch is protected
 */
function isProtectedBranch(branch) {
  if (!branch) return false;
  return PROTECTED_BRANCHES.includes(branch.trim());
}

/**
 * Validate branch naming convention
 */
function validateBranchName(branchName) {
  if (!branchName) {
    return { valid: true };
  }

  // Remove 'origin/' prefix if present
  const cleanBranch = branchName.replace(/^origin\//, '');

  // Special branches are always valid
  if (PROTECTED_BRANCHES.includes(cleanBranch) || cleanBranch === 'develop' || cleanBranch === 'master') {
    return { valid: true };
  }

  // Validate against pattern
  const isValid = BRANCH_NAME_PATTERN.test(cleanBranch);

  if (!isValid) {
    return {
      valid: false,
      errorMessage: `Invalid branch name: "${branchName}"

Expected format: <type>/<ID>-<description>

Examples:
  ✓ feat/DB-101-user-auth
  ✓ fix/DB-205-login-error
  ✓ refactor/DB-301-cache-layer
  ✓ docs/DB-401-api-docs
  ✓ hotfix/DB-801-security-patch

Types: feat, fix, refactor, docs, style, test, chore, perf, ci, hotfix
ID Format: DB-XXX (or GH-XXX for GitHub issues)
Description: kebab-case, lowercase, 3-30 characters

Please rename your branch:
  git branch -m ${cleanBranch} feat/DB-XXX-your-description

Or create a new branch:
  git checkout -b feat/DB-XXX-your-description`
    };
  }

  return { valid: true };
}

/**
 * Validate commit message format
 */
function validateCommitMessage(message) {
  if (!message) return { valid: true };

  const cleanMessage = message.trim().split('\n')[0]; // Only check first line
  const isValid = COMMIT_PATTERN.test(cleanMessage);

  if (!isValid) {
    return {
      valid: false,
      errorMessage: `Invalid commit message format: "${cleanMessage}"

Expected format: <type>(<scope>): <subject>

Types: feat, fix, refactor, docs, style, test, chore, perf, ci
Scopes: auth, system, gateway, resource, workflow, job, monitor, common, api, infra, build

Examples:
  ✓ feat(auth): add user login endpoint
  ✓ fix(gateway): resolve circuit breaker issue
  ✓ refactor(cache): improve performance
  ✓ docs(readme): update setup guide
  ✓ test(auth): add login unit tests
  ✓ style(format): apply google java format
  ✓ chore(deps): upgrade spring boot to 3.2

Please rewrite your commit message to follow this format.`
    };
  }

  return { valid: true };
}

/**
 * Main validation function
 */
function validate(toolName, args) {
  const command = args.command || args.prompt || '';

  // Skip validation for certain tools
  const skipTools = ['Read', 'Glob', 'Grep', 'Task', 'AskUserQuestion', 'Skill', 'TodoWrite', 'WebSearch'];
  if (skipTools.includes(toolName)) {
    return { valid: true };
  }

  // ========== BRANCH CREATION VALIDATION ==========
  if (command.includes('git checkout -b') || command.includes('git branch ')) {
    const branchMatch = command.match(/(?:-b\s+|branch\s+)([\w\-/]+)/);
    if (branchMatch) {
      const result = validateBranchName(branchMatch[1]);
      if (!result.valid) {
        return result;
      }
    }
  }

  // ========== COMMIT TO PROTECTED BRANCH VALIDATION ==========
  if (command.includes('git commit') && !command.includes('--amend')) {
    // Check if we're on a protected branch
    // This is a simplified check - in practice you'd run git branch --show-current
    const currentBranch = getCurrentBranch(command);

    // If we can't determine the branch, warn but don't block
    if (currentBranch && isProtectedBranch(currentBranch)) {
      return {
        valid: false,
        errorMessage: `Cannot commit directly to protected branch: "${currentBranch}"

Protected branches: ${PROTECTED_BRANCHES.join(', ')}

Please create a feature branch first:

  1. Create a new branch:
     git checkout -b feat/DB-XXX-your-description

  2. Make your changes and commit

  3. Push and create a PR:
     git push -u origin feat/DB-XXX-your-description
     /skill create-pr

  4. After PR approval, merge to ${currentBranch}`
      };
    }
  }

  // ========== COMMIT MESSAGE VALIDATION ==========
  if (command.includes('git commit') && command.includes('-m')) {
    const messageMatch = command.match(/-m\s+['"](.+?)['"]/);
    if (messageMatch) {
      const result = validateCommitMessage(messageMatch[1]);
      if (!result.valid) {
        return result;
      }
    }
  }

  // ========== FORCE PUSH TO PROTECTED BRANCH ==========
  if (command.includes('git push') && command.includes('--force')) {
    // Extract branch from command
    const branchMatch = command.match(/push.*\s+([a-zA-Z0-9/_-]+)\s*$/);
    if (branchMatch) {
      const branch = branchMatch[1].replace(/^origin\//, '');
      if (isProtectedBranch(branch)) {
        return {
          valid: false,
          errorMessage: `Force push to protected branch "${branch}" is not allowed!

Protected branches: ${PROTECTED_BRANCHES.join(', ')}

If you need to update history, create a new branch and merge instead.`
        };
      }
    }
  }

  // All validations passed
  return { valid: true };
}

// Export for use
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { validate };
}

// Return validation result
return validate(process.env.CLAUDE_TOOL_NAME || 'Bash', process.argv);
