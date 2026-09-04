package com.salardev.autodash;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.HostValidator;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.model.Action;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

public class AutoDashCarAppService extends CarAppService {
    @NonNull
    @Override
    public HostValidator createHostValidator() {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @Override
    public Session onCreateSession() {
        return new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull Intent intent) {
                return new AutoDashScreen(getCarContext());
            }
        };
    }

    private static class AutoDashScreen extends Screen {
        AutoDashScreen(@NonNull CarContext carContext) {
            super(carContext);
        }

        @NonNull
        @Override
        public Template onGetTemplate() {
            Pane pane = new Pane.Builder()
                    .addRow(new Row.Builder()
                            .setTitle("AutoDash")
                            .addText("Android Auto mode is connected")
                            .build())
                    .addRow(new Row.Builder()
                            .setTitle("Navigation")
                            .addText("Use GPS and start navigation from the car display")
                            .build())
                    .addRow(new Row.Builder()
                            .setTitle("Media")
                            .addText("Open your supported audio controls")
                            .build())
                    .addAction(new Action.Builder()
                            .setTitle("Open AutoDash")
                            .setOnClickListener(() -> invalidate())
                            .build())
                    .build();

            return new PaneTemplate.Builder(pane)
                    .setTitle("AutoDash")
                    .build();
        }
    }
}
