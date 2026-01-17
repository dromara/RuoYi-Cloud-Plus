/**
 * Dragonboat-Backend postToolUse Hook
 *
 * Automatically triggers workflow actions after tool execution.
 *
 * Features:
 * - Auto-trigger code review after PR creation
 * - Detect feature completion signals
 * - Suggest next actions in workflow
 * - Track session progress
 */

/**
 * Completion detection patterns
 */
const COMPLETION_PATTERNS = [
  'feature complete',
  '完成开发',
  'ready for review',
  'ready for pr',
  'prepared for review',
  '开发完成',
  '功能完成',
  'pr ready',
  'can review',
  'please review'
];

/**
 * PR creation patterns
 */
const PR_PATTERNS = [
  'gh pr create',
  'create-pr',
  'create pull request',
  'new pull request',
  '/skill create-pr'
];

/**
 * Commit patterns that might indicate readiness
 */
const COMMIT_PATTERNS = [
  'feat(', 'fix(', 'refactor('
];

/**
 * Detect if a command/result indicates feature completion
 */
function detectFeatureCompletion(result) {
  if (!result || !result.output) return false;

  const output = typeof result.output === 'string'
    ? result.output.toLowerCase()
    : JSON.stringify(result.output).toLowerCase();

  return COMPLETION_PATTERNS.some(pattern => output.includes(pattern.toLowerCase()));
}

/**
 * Detect if a command creates a PR
 */
function detectPRCreation(command, toolName) {
  if (!command) return false;

  const cmd = typeof command === 'string' ? command.toLowerCase() : JSON.stringify(command).toLowerCase();

  // Check for PR creation commands
  if (PR_PATTERNS.some(pattern => cmd.includes(pattern.toLowerCase()))) {
    return true;
  }

  // Check for create-pr skill invocation
  if (toolName === 'Skill' && cmd.includes('create-pr')) {
    return true;
  }

  return false;
}

/**
 * Parse PR URL from output
 */
function extractPRUrl(output) {
  if (!output) return null;

  // GitHub CLI format: https://github.com/owner/repo/pull/123
  const urlMatch = output.match(/https:\/\/github\.com\/[\w-]+\/[\w-]+\/pull\/\d+/);
  if (urlMatch) return urlMatch[0];

  // gh CLI output format: Pull request #123 created
  const prMatch = output.match(/pull request #(\d+)/);
  if (prMatch) {
    return `#${prMatch[1]}`;
  }

  return null;
}

/**
 * Generate code review trigger suggestion
 */
function suggestCodeReview(context) {
  const suggestions = [];

  suggestions.push({
    action: 'trigger_code_review',
    skill: 'ry-code-review',
    priority: 'high',
    message: 'Feature development complete. Consider running code review.'
  });

  if (context.prUrl) {
    suggestions.push({
      action: 'review_pr',
      tool: 'code-review',
      args: context.prUrl,
      priority: 'high',
      message: `PR created: ${context.prUrl}. Run code review now?`
    });
  }

  return suggestions;
}

/**
 * Generate next step suggestions
 */
function suggestNextSteps(toolName, context) {
  const steps = [];

  // After branch creation
  if (toolName === 'Bash' && context.command && context.command.includes('git checkout -b')) {
    steps.push({
      action: 'start_development',
      message: 'Branch created successfully. Next steps:',
      steps: [
        '1. Write code or use /skill ry-crud to generate CRUD',
        '2. Test your changes locally',
        '3. Use /skill commit-commands:commit to commit',
        '4. Use /skill create-pr to create pull request'
      ]
    });
  }

  // After commit
  if (toolName === 'Bash' && context.command && context.command.includes('git commit')) {
    steps.push({
      action: 'after_commit',
      message: 'Commit created. Next steps:',
      steps: [
        '1. Push changes: git push',
        '2. Create PR: /skill create-pr',
        '3. Or continue development'
      ]
    });
  }

  // After PR creation
  if (detectPRCreation(context.command, toolName)) {
    steps.push({
      action: 'after_pr',
      message: 'PR created. Next steps:',
      steps: [
        '1. Wait for CI checks to complete',
        '2. Review automated code review feedback',
        '3. Address any issues found',
        '4. Request team review',
        '5. Merge after approval'
      ]
    });
  }

  return steps;
}

/**
 * Main postToolUse handler
 */
function handle(toolName, args, result) {
  const context = {
    toolName,
    command: args.command || args.prompt || '',
    result: result,
    prUrl: null,
    timestamp: new Date().toISOString()
  };

  const actions = [];
  const messages = [];

  // Detect PR creation
  if (detectPRCreation(context.command, toolName)) {
    context.prUrl = extractPRUrl(result?.output);

    messages.push({
      type: 'success',
      title: 'PR Detected',
      content: context.prUrl
      ? `Pull request created: ${context.prUrl}`
      : 'Pull request creation detected'
    });

    // Suggest code review
    const reviewSuggestions = suggestCodeReview(context);
    actions.push(...reviewSuggestions);
  }

  // Detect feature completion
  if (detectFeatureCompletion(result) || detectFeatureCompletion({ output: context.command })) {
    messages.push({
      type: 'info',
      title: 'Feature Completion Detected',
      content: 'It looks like you\'ve completed a feature. Ready for code review?'
    });

    const reviewSuggestions = suggestCodeReview(context);
    actions.push(...reviewSuggestions);
  }

  // Suggest next steps
  const nextSteps = suggestNextSteps(toolName, context);
  if (nextSteps.length > 0) {
    messages.push(...nextSteps);
  }

  // Track session activity
  const sessionNote = trackSessionActivity(toolName, context);

  return {
    actions,
    messages,
    sessionNote
  };
}

/**
 * Track session activity for summary
 */
function trackSessionActivity(toolName, context) {
  const activity = {
    timestamp: context.timestamp,
    tool: toolName,
    command: context.command.substring(0, 100) // Truncate for readability
  };

  // Track branches
  if (context.command.includes('git checkout -b')) {
    activity.type = 'branch_created';
  }

  // Track commits
  if (context.command.includes('git commit')) {
    activity.type = 'commit_created';
  }

  // Track PRs
  if (detectPRCreation(context.command, toolName)) {
    activity.type = 'pr_created';
    activity.prUrl = context.prUrl;
  }

  return activity;
}

/**
 * Format output for user
 */
function formatOutput(response) {
  if (!response) return '';

  let output = '';

  // Display messages
  if (response.messages && response.messages.length > 0) {
    response.messages.forEach(msg => {
      if (msg.type === 'success') {
        output += `\n✓ ${msg.title}\n`;
        if (msg.content) output += `  ${msg.content}\n`;
      } else if (msg.type === 'info') {
        output += `\nℹ ${msg.title}\n`;
        if (msg.content) output += `  ${msg.content}\n`;
      } else if (msg.steps) {
        output += `\n${msg.message}\n`;
        msg.steps.forEach(step => {
          output += `  ${step}\n`;
        });
      }
    });
  }

  // Display action suggestions
  if (response.actions && response.actions.length > 0) {
    output += `\n📋 Suggested Actions:\n`;
    response.actions.forEach((action, i) => {
      output += `  ${i + 1}. `;
      if (action.skill) {
        output += `Run: /skill ${action.skill}`;
      } else if (action.tool) {
        output += `Run: ${action.tool}`;
        if (action.args) {
          output += ` ${action.args}`;
        }
      }
      output += `\n     ${action.message}\n`;
    });
  }

  return output;
}

// Export for use
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { handle, detectFeatureCompletion, detectPRCreation, suggestCodeReview };
}

// Main execution
const toolName = process.env.CLAUDE_TOOL_NAME || 'Bash';
const args = process.argv || {};
const result = {}; // Would be populated with actual result

const response = handle(toolName, args, result);

// Print formatted output
if (Object.keys(response).length > 0) {
  console.log(formatOutput(response));
}

return response;
