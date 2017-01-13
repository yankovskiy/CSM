package ru.neverdark.csm.async;

import android.os.AsyncTask;

public class ExportToGpxTask extends AsyncTask<Integer, Void, Void> {
    /**
     * экспорт указанной тренировки в GPX-файл
     * @param trainingId id тренировки из базы
     * @return Void
     */
    @Override
    protected Void doInBackground(Integer... trainingId) {
        // TODO экспорт в GPX
        return null;
    }
}
