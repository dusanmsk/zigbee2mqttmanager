package org.msk.zigbee2mqtt.manager.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

@Route
@RequiredArgsConstructor
@UIScope
@Slf4j
public class MainView extends VerticalLayout {

    private final EditMappingForm editMappingForm;

    @PostConstruct
    public void init() {
        setupUI();
    }

    private void setupUI() {

        add(editMappingForm);

    }

}
