package science.keng42.keep.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import science.keng42.keep.EntryActivity;

/**
 * Created by Keng on 2015/6/12
 */
public class NewEntryService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        stopSelf(startId);
        startMain();
        return START_STICKY;
    }

    private void startMain() {
        Intent intent = new Intent(this, EntryActivity.class);
        intent.putExtra("position", -1);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
