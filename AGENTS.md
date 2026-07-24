# AI Agent Instructions

This file contains agent-specific instructions and conventions for working with this project.

## Shared Context

**IMPORTANT**: Before working on this project, read the shared context in [CONTEXT.md](./CONTEXT.md).

CONTEXT.md contains:
- Project overview and architecture
- Tech stack details
- Code conventions and style guidelines
- Package structure
- Key design patterns
- Development workflow
- Common tasks and examples

## Bob Shell Mode Selection

When using Bob Shell, select the appropriate mode based on the task:

### Planning & Design Tasks → `plan` mode
- Creating project specifications
- Designing system architecture
- Planning implementation approaches
- Breaking down complex features
- Creating technical documentation

### Implementation & Coding → `code` mode
- Writing new code
- Modifying existing code
- Refactoring
- Bug fixes
- File operations (create, update, delete)

### Questions & Explanations → `ask` mode
- Answering questions about the codebase
- Explaining how something works
- Providing documentation
- Clarifying requirements
- Searching documentation

### Complex Multi-File Changes → `advanced` mode
- Large-scale refactoring across multiple files
- Complex feature implementation spanning many components
- System-wide changes requiring coordination

## Agent-Specific Guidelines

### For Claude (via .claude/ directory)
- Use commands in `.claude/commands/` for structured workflows
- Follow spec-driven development: spec → plan → tasks → implement
- Maintain specifications in `specs/` directory

### For Bob Shell
- Leverage mode-specific capabilities
- Use `search_docs` tool for Bob Shell documentation questions
- Follow the tool use format strictly (XML tags)
- Always provide absolute paths to tools

## Project-Specific Rules

1. **Test-First Approach**: Write tests before implementation when adding new features
2. **Mode Awareness**: When modifying ingestion logic, understand which mode (A/B/C) you're affecting
3. **Provenance Preservation**: Always maintain document provenance in chunks (source, page, method)
4. **Configuration Over Code**: Check `application.yml` before hardcoding values
5. **Error Handling**: Use domain-specific exceptions (e.g., `IngestionException`)

## Common Workflows

### Adding a New Feature
1. Switch to `plan` mode to design the feature
2. Create specification in `specs/` directory
3. Switch to `code` mode for implementation
4. Write tests first, then implementation
5. Verify with existing test suite

### Debugging an Issue
1. Use `code` mode to add debug logging
2. Run tests to reproduce the issue
3. Analyze logs and test output
4. Implement fix with test coverage
5. Verify fix doesn't break existing functionality

### Answering Questions
1. Switch to `ask` mode
2. Use `search_docs` for Bob Shell questions
3. Read relevant source files for project questions
4. Provide clear, concise answers with code examples

## Integration Notes

- **CONTEXT.md**: Contains shared project context (read-only, synchronized with CLAUDE.md)
- **AGENTS.md**: This file - agent-specific instructions and mode selection rules
- **CLAUDE.md**: Symlink to CONTEXT.md for Claude compatibility
