package ru.neverdark.csm.components;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.TextView;

import java.util.Locale;

import ru.neverdark.csm.R;

/**
 * Based on the compass (https://github.com/iutinvg/compass) by Viacheslav Iutin
 */
public class Compass implements SensorEventListener {
    private static final String TAG = "Compass";

    private SensorManager mSensorManager;
    private Sensor mGsensor;
    private Sensor mMFsensor;
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float mAzimuth = 0f;
    private float mCurrectAzimuth = 0f;
    private View mArrowView = null;
    private boolean mIsStarted;
    private TextView mDeegres;
    private TextView mDirection;

    public Compass(Context context) {
        mSensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        mGsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMFsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void start() {
        if (mArrowView == null) {
            throw new NullPointerException("arrow view is not set");
        }

        if (mDeegres == null) {
            throw new NullPointerException("Deegres TextView is not set");
        }

        if (mDirection == null) {
            throw new NullPointerException("Direction TextView is not set");
        }

        mSensorManager.registerListener(this, mGsensor,
                SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMFsensor,
                SensorManager.SENSOR_DELAY_GAME);
        mIsStarted = true;
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
        mIsStarted = false;
    }

    public boolean isStarted() {
        return mIsStarted;
    }

    public void setDeegresTv(TextView tv) {
        mDeegres = tv;
    }

    public void setDirectionTv(TextView tv) {
        mDirection = tv;
    }

    public void setArrowView(View arrowView) {
        mArrowView = arrowView;
    }

    private void adjustArrow() {
        Log.v(TAG, "will set rotation from " + mCurrectAzimuth + " to "
                + mAzimuth);

        Animation an = new RotateAnimation(-mCurrectAzimuth, -mAzimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mCurrectAzimuth = mAzimuth;

        an.setDuration(500);
        an.setRepeatCount(0);
        an.setFillAfter(true);

        mArrowView.startAnimation(an);
        mDeegres.setText(String.format(Locale.US, "%d°", Math.round(mCurrectAzimuth)));
        mDirection.setText(getDirectionByAzimuth(mCurrectAzimuth));
    }

    private int getDirectionByAzimuth(float azimuth) {
        if ((azimuth >= 0 && azimuth <= 10) || (azimuth >= 350 && azimuth <= 360)) {
            return R.string.north_direction;
        }

        if (azimuth > 10 && azimuth < 80) {
            return R.string.northeast_direction;
        }

        if (azimuth >= 80 && azimuth <= 100) {
            return R.string.east_direction;
        }

        if (azimuth > 100 && azimuth < 170) {
            return R.string.southeast_direction;
        }

        if (azimuth >= 170 && azimuth <= 190) {
            return R.string.south_direction;
        }

        if (azimuth > 190 && azimuth < 260) {
            return R.string.southwest_direction;
        }

        if (azimuth >= 260 && azimuth <= 280) {
            return R.string.west_direction;
        }

        if (azimuth > 280 && azimuth < 350) {
            return R.string.northwest_direction;
        }

        return R.string.north_direction;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.97f;

        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                mGravity[0] = alpha * mGravity[0] + (1 - alpha)
                        * event.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha)
                        * event.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha)
                        * event.values[2];

                // mGravity = event.values;

                // Log.e(TAG, Float.toString(mGravity[0]));
            }

            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                // mGeomagnetic = event.values;

                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha)
                        * event.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha)
                        * event.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha)
                        * event.values[2];
                // Log.e(TAG, Float.toString(event.values[0]));

            }

            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
                    mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                // Log.d(TAG, "mAzimuth (rad): " + mAzimuth);
                mAzimuth = (float) Math.toDegrees(orientation[0]); // orientation
                mAzimuth = (mAzimuth + 360) % 360;
                // Log.d(TAG, "mAzimuth (deg): " + mAzimuth);
                adjustArrow();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}