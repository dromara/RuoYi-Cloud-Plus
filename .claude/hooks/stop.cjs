/**
 * Dragonboat-Backend stop Hook
 *
 * Generates a comprehensive session summary report when the session ends.
 *
 * Features:
 * - Summarize files modified during session
 * - Track commits and branches created
 * - List pending tasks
 * - Provide recommendations for next steps
 * - Generate activity timeline
 */

/**
 * Session state tracker
 */
const sessionState = {
  startTime: new Date(),
  activities: [],
  filesModified: new Set(),
  branchesCreated: new Set(),
  commitsCreated: [],
  prsCreated: [],
  issuesFound: [],
  recommendations: []
};

/**
 * Record activity during session
 */
function recordActivity(type, data) {
  sessionState.activities.push({
    type,
    timestamp: new Date(),
    ...data
  });

  // Update specific collections
  switch (type) {
    case 'file_modified':
      sessionState.filesModified.add(data.path);
      break;
    case 'branch_created':
      sessionState.branchesCreated.add(data.branch);
      break;
    case 'commit_created':
      sessionState.commitsCreated.push({
        hash: data.hash,
        message: data.message,
        branch: data.branch,
        timestamp: data.timestamp
      });
      break;
    case 'pr_created':
      sessionState.prsCreated.push({
        number: data.number,
        url: data.url,
        title: data.title,
        branch: data.branch
      });
      break;
    case 'issue_found':
      sessionState.issuesFound.push({
        severity: data.severity,
        description: data.description,
        file: data.file
      });
      break;
    case 'recommendation':
      sessionState.recommendations.push({
        priority: data.priority,
        action: data.action,
        reason: data.reason
      });
      break;
  }
}

/**
 * Generate session summary
 */
function generateSummary() {
  const endTime = new Date();
  const duration = endTime - sessionState.startTime;

  const summary = {
    sessionInfo: {
      startTime: sessionState.startTime.toISOString(),
      endTime: endTime.toISOString(),
      duration: formatDuration(duration),
      totalActivities: sessionState.activities.length
    },
    filesModified: Array.from(sessionState.filesModified),
    branchesCreated: Array.from(sessionState.branchesCreated),
    commitsCreated: sessionState.commitsCreated,
    prsCreated: sessionState.prsCreated,
    issuesFound: sessionState.issuesFound,
    recommendations: sessionState.recommendations,
    activityTimeline: generateTimeline()
  };

  return summary;
}

/**
 * Format duration in human-readable format
 */
function formatDuration(ms) {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);

  if (hours > 0) {
    return `${hours}h ${minutes % 60}m`;
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`;
  } else {
    return `${seconds}s`;
  }
}

/**
 * Generate activity timeline
 */
function generateTimeline() {
  const timeline = [];
  const typeGroups = {};

  sessionState.activities.forEach(activity => {
    if (!typeGroups[activity.type]) {
      typeGroups[activity.type] = [];
    }
    typeGroups[activity.type].push(activity);
  });

  Object.keys(typeGroups).forEach(type => {
    timeline.push({
      type,
      count: typeGroups[type].length,
      items: typeGroups[type]
    });
  });

  return timeline;
}

/**
 * Format summary for display
 */
function formatSummary(summary) {
  let output = '\n';
  output += '=' .repeat(80) + '\n';
  output += 'DRAGONBOAT-BACKEND SESSION SUMMARY\n';
  output += '='.repeat(80) + '\n\n';

  // Session info
  output += '📊 Session Information\n';
  output += '-' .repeat(40) + '\n';
  output += `Start Time:    ${summary.sessionInfo.startTime}\n`;
  output += `End Time:      ${summary.sessionInfo.endTime}\n`;
  output += `Duration:      ${summary.sessionInfo.duration}\n`;
  output += `Total Actions: ${summary.sessionInfo.totalActivities}\n\n`;

  // Files modified
  if (summary.filesModified.length > 0) {
    output += `📝 Files Modified (${summary.filesModified.length})\n`;
    output += '-'.repeat(40) + '\n';
    summary.filesModified.forEach(file => {
      output += `  ✓ ${file}\n`;
    });
    output += '\n';
  }

  // Branches created
  if (summary.branchesCreated.length > 0) {
    output += `🌿 Branches Created (${summary.branchesCreated.length})\n`;
    output += '-'.repeat(40) + '\n';
    summary.branchesCreated.forEach(branch => {
      output += `  ✓ ${branch}\n`;
    });
    output += '\n';
  }

  // Commits created
  if (summary.commitsCreated.length > 0) {
    output += `💾 Commits Created (${summary.commitsCreated.length})\n`;
    output += '-'.repeat(40) + '\n';
    summary.commitsCreated.forEach(commit => {
      output += `  ✓ ${commit.hash} - ${commit.message}\n`;
    });
    output += '\n';
  }

  // PRs created
  if (summary.prsCreated.length > 0) {
    output += `🔀 Pull Requests Created (${summary.prsCreated.length})\n`;
    output += '-'.repeat(40) + '\n';
    summary.prsCreated.forEach(pr => {
      output += `  ✓ #${pr.number} - ${pr.title}\n`;
      output += `    ${pr.url}\n`;
    });
    output += '\n';
  }

  // Issues found
  if (summary.issuesFound.length > 0) {
    const critical = summary.issuesFound.filter(i => i.severity === 'critical').length;
    const major = summary.issuesFound.filter(i => i.severity === 'major').length;
    const minor = summary.issuesFound.filter(i => i.severity === 'minor').length;

    output += `⚠️  Issues Found (${summary.issuesFound.length})\n`;
    output += '-'.repeat(40) + '\n';
    output += `  Critical: ${critical} | Major: ${major} | Minor: ${minor}\n`;
    summary.issuesFound.forEach(issue => {
      const icon = issue.severity === 'critical' ? '🔴' : issue.severity === 'major' ? '🟡' : '🟢';
      output += `  ${icon} [${issue.severity.toUpperCase()}] ${issue.description}\n`;
      if (issue.file) {
        output += `     File: ${issue.file}\n`;
      }
    });
    output += '\n';
  }

  // Recommendations
  if (summary.recommendations.length > 0) {
    output += `💡 Recommendations (${summary.recommendations.length})\n`;
    output += '-'.repeat(40) + '\n';
    summary.recommendations.forEach((rec, i) => {
      const icon = rec.priority === 'high' ? '🔴' : rec.priority === 'medium' ? '🟡' : '🟢';
      output += `  ${icon} ${i + 1}. ${rec.action}\n`;
      output += `     Reason: ${rec.reason}\n`;
    });
    output += '\n';
  }

  // Next steps
  output += '🚀 Suggested Next Steps\n';
  output += '-'.repeat(40) + '\n';
  output += generateNextSteps(summary);
  output += '\n';

  // Footer
  output += '='.repeat(80) + '\n';
  output += 'Generated by Dragonboat-Backend Workflow System\n';
  output += `Report Generated: ${new Date().toISOString()}\n`;
  output += '='.repeat(80) + '\n';

  return output;
}

/**
 * Generate next steps based on session state
 */
function generateNextSteps(summary) {
  const steps = [];

  // If no commits, suggest committing
  if (summary.commitsCreated.length === 0 && summary.filesModified.length > 0) {
    steps.push('1. Stage and commit your changes:');
    steps.push('   git add .');
    steps.push('   git commit -m "feat(scope): description"');
    steps.push('');
  }

  // If commits but no PR, suggest creating PR
  if (summary.commitsCreated.length > 0 && summary.prsCreated.length === 0) {
    steps.push('1. Push your commits:');
    steps.push('   git push -u origin $(git branch --show-current)');
    steps.push('');
    steps.push('2. Create a pull request:');
    steps.push('   /skill create-pr');
    steps.push('');
  }

  // If PR created, suggest review
  if (summary.prsCreated.length > 0) {
    steps.push('1. Wait for CI checks to complete on your PR');
    steps.push('');
    steps.push('2. Review the automated code review feedback');
    steps.push('');
    steps.push('3. Address any issues found by the review');
    steps.push('');
  }

  // If issues found, suggest fixing
  if (summary.issuesFound.length > 0) {
    const criticalIssues = summary.issuesFound.filter(i => i.severity === 'critical').length;
    const majorIssues = summary.issuesFound.filter(i => i.severity === 'major').length;

    if (criticalIssues > 0 || majorIssues > 0) {
      steps.push(`1. Fix ${criticalIssues} critical and ${majorIssues} major issues found during review`);
      steps.push('');
    }
  }

  // Default next steps
  if (steps.length === 0) {
    steps.push('1. Continue development or use workflow commands');
    steps.push('');
    steps.push('Available commands:');
    steps.push('  /skill ry-workflow      - Start a new feature');
    steps.push('  /skill ry-branch-management - Manage branches');
    steps.push('  /skill ry-code-review   - Run code review');
    steps.push('  /skill commit-commands:commit - Commit changes');
    steps.push('  /skill create-pr        - Create pull request');
  }

  return steps.join('\n');
}

/**
 * Main stop handler
 */
function handleStop(context) {
  // Generate summary
  const summary = generateSummary();

  // Format for display
  const formatted = formatSummary(summary);

  // Return both raw and formatted
  return {
    summary,
    formatted,
    timestamp: new Date().toISOString()
  };
}

// Export for use
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    recordActivity,
    generateSummary,
    formatSummary,
    handleStop
  };
}

// Main execution
const result = handleStop({});

// Print formatted summary
console.log(result.formatted);

return result;
