# Spec 007: Right-Justify User Messages — Technical Plan

**Status:** Approved

## Approach

Two changes, following the existing `.highlighted` pattern:

### 1. Java — add class name to user messages

In `ChatPanel.java`, after creating the user `MessageListItem`, add:

```java
userItem.addClassNames("current-user");
```

### 2. CSS — right-justify via flexbox reversal

In `styles.css`, add rules targeting `vaadin-message.current-user`:

```css
vaadin-message.current-user {
    flex-direction: row-reverse;
}

vaadin-message.current-user::part(content) {
    align-items: flex-end;
}

vaadin-message.current-user::part(header) {
    flex-direction: row-reverse;
}
```

- `flex-direction: row-reverse` on the root moves the avatar to the
  right
- `align-items: flex-end` on `::part(content)` right-aligns the text
  block
- `flex-direction: row-reverse` on `::part(header)` puts the name on
  the right side of the header row

## Files to modify

| File | Change |
|------|--------|
| `ChatPanel.java` | Add `userItem.addClassNames("current-user")` |
| `styles.css` | Add 3 CSS rules for `.current-user` |

## Test approach

- `ChatPanelTest`: verify user message items have the `current-user`
  class name

## Verification

Start the app, open any mode, send a message. The "You" message should
appear right-aligned with the avatar on the right. Assistant response
should remain left-aligned.
