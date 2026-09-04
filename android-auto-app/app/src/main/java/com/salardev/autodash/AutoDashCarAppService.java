package com.salardev.autodash;

import android.app.ActivityOptions;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.HostValidator;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.model.Action;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.Template;

public class AutoDashCarAppService extends CarAppService {

    @NonNull
    @Override
    public HostValidator createHostValidator() {
        // Keep this permissive for local Android Auto testing.
        // Replace with an allow-list validator before production release.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @Override
    public Session onCreateSession() {
        return new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull Intent intent) {
                return new HomeScreen(getCarContext());
            }

            @Override
            public void onNewIntent(@NonNull Intent intent) {
                super.onNewIntent(intent);
                if (CarContext.ACTION_NAVIGATE.equals(intent.getAction())) {
                    getCarContext().getCarService(androidx.car.app.AppManager.class)
                            .showToast("Navigation request received", Toast.LENGTH_SHORT);
                }
            }
        };
    }

    private static final class HomeScreen extends Screen {
        HomeScreen(@NonNull CarContext carContext) {
            super(carContext);
        }

        @NonNull
        @Override
        public Template onGetTemplate() {
            ItemList list = new ItemList.Builder()
                    .addItem(new Row.Builder()
                            .setTitle("Maps / Navigation")
                            .addText("Search a destination and open a compatible navigation app")
                            .setOnClickListener(() -> getScreenManager().push(new DestinationSearchScreen(getCarContext())))
                            .build())
                    .addItem(new Row.Builder()
                            .setTitle("Music")
                            .addText("Open the phone's installed music app")
                            .setOnClickListener(this::openMusicApp)
                            .build())
                    .addItem(new Row.Builder()
                            .setTitle("AutoDash")
                            .addText("Android Auto connection is active")
                            .build())
                    .build();

            return new ListTemplate.Builder()
                    .setTitle("AutoDash")
                    .setSingleList(list)
                    .addAction(new Action.Builder()
                            .setTitle("Refresh")
                            .setOnClickListener(this::invalidate)
                            .build())
                    .build();
        }

        private void openMusicApp() {
            Intent musicIntent = Intent.makeMainSelectorActivity(
                    Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_MUSIC
            );
            musicIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ActivityOptions options = ActivityOptions.makeBasic();
                    options.setLaunchDisplayId(Display.DEFAULT_DISPLAY);
                    getCarContext().startActivity(musicIntent, options.toBundle());
                } else {
                    getCarContext().startActivity(musicIntent);
                }
            } catch (Exception e) {
                getCarContext().getCarService(androidx.car.app.AppManager.class)
                        .showToast("No compatible music app found", Toast.LENGTH_SHORT);
            }
        }
    }

    private static final class DestinationSearchScreen extends Screen {
        DestinationSearchScreen(@NonNull CarContext carContext) {
            super(carContext);
        }

        @NonNull
        @Override
        public Template onGetTemplate() {
            SearchTemplate.SearchCallback callback = new SearchTemplate.SearchCallback() {
                @Override
                public void onSearchSubmitted(@NonNull String searchText) {
                    String destination = searchText.trim();
                    if (destination.isEmpty()) {
                        return;
                    }

                    String encoded = Uri.encode(destination);
                    Intent navigationIntent = new Intent(
                            CarContext.ACTION_NAVIGATE,
                            Uri.parse("geo:0,0?q=" + encoded)
                    );

                    try {
                        // Android Auto routes ACTION_NAVIGATE to the user's
                        // compatible navigation app rather than hard-coding Google Maps.
                        getCarContext().startCarApp(navigationIntent);
                    } catch (Exception e) {
                        getScreenManager().push(new MessageScreen(
                                getCarContext(),
                                "Navigation unavailable",
                                "No compatible navigation app could handle this destination."
                        ));
                    }
                }
            };

            return new SearchTemplate.Builder(callback)
                    .setSearchHint("Enter destination")
                    .setShowKeyboardByDefault(true)
                    .setHeaderAction(Action.BACK)
                    .build();
        }
    }

    private static final class MessageScreen extends Screen {
        private final String title;
        private final String message;

        MessageScreen(@NonNull CarContext carContext, String title, String message) {
            super(carContext);
            this.title = title;
            this.message = message;
        }

        @NonNull
        @Override
        public Template onGetTemplate() {
            return new MessageTemplate.Builder(message)
                    .setTitle(title)
                    .setHeaderAction(Action.BACK)
                    .build();
        }
    }
}
