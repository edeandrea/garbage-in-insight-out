package dev.ericdeandrea.docling.ui;

import java.util.EnumMap;
import java.util.Map;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Border;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderColor;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Position;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;
import com.vaadin.flow.theme.lumo.LumoUtility.ZIndex;

import dev.ericdeandrea.docling.ai.AssistantService;
import dev.ericdeandrea.docling.model.Mode;

@Route("")
@PageTitle("Garbage In, Insight Out")
public class ChatView extends VerticalLayout {

    private final Map<Mode, ChatPanel> panels = new EnumMap<>(Mode.class);
    private final Map<Mode, Button> toggleButtons = new EnumMap<>(Mode.class);
    private final HorizontalLayout messageContainer;
    private final HorizontalLayout chunksContainer;
    private final AssistantService assistantService;
    private boolean isDarkMode;

    public ChatView(AssistantService assistantService) {
        this.assistantService = assistantService;

        setSizeFull();
        setPadding(false);

        var toolbar = createToolbar();

        this.messageContainer = new HorizontalLayout();
        this.messageContainer.setSizeFull();
        this.messageContainer.setSpacing(true);

        this.chunksContainer = new HorizontalLayout();
        this.chunksContainer.setSizeFull();
        this.chunksContainer.setSpacing(true);

        var splitLayout = new SplitLayout(Orientation.VERTICAL);
        splitLayout.setSizeFull();
        splitLayout.addToPrimary(this.messageContainer);
        splitLayout.addToSecondary(this.chunksContainer);
        splitLayout.setSplitterPosition(70);

        add(toolbar, splitLayout);
        expand(splitLayout);

        toggleMode(Mode.NAIVE);
    }

    private VerticalLayout createToolbar() {
        var title = new H2("Garbage In, Insight Out");
        title.addClassNames(Whitespace.NOWRAP, Margin.NONE);
        title.getStyle().set("font-size", "var(--lumo-font-size-xl)");

        var themeToggle = new Button(VaadinIcon.ADJUST.create(), _ -> toggleTheme());
        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeToggle.setTooltipText("Toggle light/dark mode");

        var topRow = new HorizontalLayout(title, themeToggle);
        topRow.setWidthFull();
        topRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        topRow.setAlignItems(FlexComponent.Alignment.CENTER);
        topRow.setFlexShrink(0, title);
        topRow.expand(title);
        title.getStyle().set("text-align", "center");

        var bottomRow = new HorizontalLayout();
        bottomRow.setWidthFull();
        bottomRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        for (var mode : Mode.values()) {
            var button = new Button(mode.displayLabel(), _ -> toggleMode(mode));
            this.toggleButtons.put(mode, button);
            bottomRow.add(button);
        }

        var toolbar = new VerticalLayout(topRow, bottomRow);
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(false);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);

        toolbar.addClassNames(
            "sticky-toolbar",
            Position.STICKY,
            Position.Top.NONE,
            ZIndex.SMALL,
            Border.BOTTOM,
            BorderColor.CONTRAST_30
        );

        return toolbar;
    }

    private void toggleTheme() {
        this.isDarkMode = !this.isDarkMode;
        UI.getCurrent().getPage().setColorScheme(
          this.isDarkMode ? ColorScheme.Value.DARK:ColorScheme.Value.LIGHT
        );
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().getPage().setColorScheme(ColorScheme.Value.LIGHT_DARK);
    }

    void toggleMode(Mode mode) {
        if (this.panels.containsKey(mode)) {
            var panel = this.panels.get(mode);
            this.messageContainer.remove(panel.messageArea());
            this.chunksContainer.remove(panel.chunksArea());
            this.panels.remove(mode);
            this.toggleButtons.get(mode).removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
        else {
            var panel = new ChatPanel(mode, this.assistantService);
            this.panels.put(mode, panel);
            this.messageContainer.add(panel.messageArea());
            this.chunksContainer.add(panel.chunksArea());
            this.toggleButtons.get(mode).addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }

        updatePanelBorders();
    }

    private void updatePanelBorders() {
        applyBordersBetween(this.messageContainer);
        applyBordersBetween(this.chunksContainer);
    }

    private void applyBordersBetween(HorizontalLayout container) {
        var children = container.getChildren().toList();

        for (var i = 0; i < children.size(); i++) {
            var style = children.get(i).getStyle();

            if ((i > 0) && (children.size() >= 2)) {
                style.set("border-inline-start", "1px solid var(--lumo-contrast-30pct)");
            }
            else {
                style.remove("border-inline-start");
            }

            style.remove("border-inline-end");
        }
    }

    Map<Mode, ChatPanel> panels() {
        return this.panels;
    }
}
