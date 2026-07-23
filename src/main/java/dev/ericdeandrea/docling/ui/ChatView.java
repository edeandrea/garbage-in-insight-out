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
import com.vaadin.flow.router.Route;

import dev.ericdeandrea.docling.ai.AssistantService;
import dev.ericdeandrea.docling.model.Mode;

@Route("")
public class ChatView extends VerticalLayout {

    private final Map<Mode, ChatPanel> panels = new EnumMap<>(Mode.class);
    private final Map<Mode, Button> toggleButtons = new EnumMap<>(Mode.class);
    private final HorizontalLayout panelContainer;
    private final AssistantService assistantService;
    private boolean isDarkMode;

    public ChatView(AssistantService assistantService) {
        this.assistantService = assistantService;

        setSizeFull();
        setPadding(true);

        var toolbar = createToolbar();
        panelContainer = new HorizontalLayout();
        panelContainer.setSizeFull();
        panelContainer.setSpacing(true);

        add(toolbar, panelContainer);
        expand(panelContainer);

        toggleMode(Mode.NAIVE);
    }

    private HorizontalLayout createToolbar() {
        var toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.add(new H2("Garbage In, Insight Out"));

        for (var mode : Mode.values()) {
            var button = new Button(mode.displayLabel(), event -> toggleMode(mode));
            toggleButtons.put(mode, button);
            toolbar.add(button);
        }

        var themeToggle = new Button(VaadinIcon.ADJUST.create(), event -> toggleTheme());
        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeToggle.setTooltipText("Toggle light/dark mode");

        var spacer = new HorizontalLayout();
        spacer.setWidthFull();
        toolbar.add(spacer, themeToggle);
        toolbar.expand(spacer);

        return toolbar;
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        UI.getCurrent().getPage().setColorScheme(
            isDarkMode ? ColorScheme.Value.DARK : ColorScheme.Value.LIGHT
        );
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().getPage().setColorScheme(ColorScheme.Value.LIGHT_DARK);
    }

    void toggleMode(Mode mode) {
        if (panels.containsKey(mode)) {
            panelContainer.remove(panels.get(mode));
            panels.remove(mode);
            toggleButtons.get(mode).removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        } else {
            var panel = new ChatPanel(mode, assistantService);
            panels.put(mode, panel);
            panelContainer.add(panel);
            toggleButtons.get(mode).addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
    }

    Map<Mode, ChatPanel> panels() {
        return panels;
    }
}
