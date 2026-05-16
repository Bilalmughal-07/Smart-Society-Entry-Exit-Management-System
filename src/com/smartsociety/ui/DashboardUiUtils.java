package com.smartsociety.ui;

import javafx.scene.control.Button;
import javafx.scene.control.TableView;

final class DashboardUiUtils {
    private DashboardUiUtils() {}

    static void initializeSidebar(Button[] buttons, int activeIndex) {
        for (Button button : buttons) {
            button.getStyleClass().removeIf(styleClass ->
                    styleClass.contains("sidebar-item") || styleClass.equals("sidebar-item-active"));
            button.getStyleClass().add("sidebar-item");
        }
        activateSidebarButton(buttons, activeIndex);
    }

    static void activateSidebarButton(Button[] buttons, int activeIndex) {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].getStyleClass().remove("sidebar-item-active");
            if (i == activeIndex) {
                buttons[i].getStyleClass().add("sidebar-item-active");
            }
        }
    }

    @SafeVarargs
    static void useConstrainedTableColumns(TableView<?>... tables) {
        for (TableView<?> table : tables) {
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }
    }
}
