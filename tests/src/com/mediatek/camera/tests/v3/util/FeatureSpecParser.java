package com.mediatek.camera.tests.v3.util;

import android.content.Context;
import android.os.Environment;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.portability.SystemProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class FeatureSpecParser {
    private static final LogUtil.Tag TAG = Utils.getTestTag(FeatureSpecParser.class.getSimpleName
            ());
    private static final String SPEC_NAME_PREFIX = "camera_at_feature_spec";
    private static final String SPEC_NAME_SEPARATOR = "_";
    private static final String SPEC_NAME_SUFFIX = ".csv";

    private Context mContext;

    private List<FeatureSpec> mFeatureSpecList = new ArrayList<>();

    public FeatureSpecParser(Context context) {
        mContext = context;

        String[] possibleSpecNames = getPossibleSpecName();
        for (String specName : possibleSpecNames) {
            FeatureSpec spec = new FeatureSpec();
            InputStream inputStream = getInputStream(specName, spec);
            if (inputStream != null) {
                spec.mSpecName = specName;
                initFeatureSpec(inputStream, spec);
                closeSafely(inputStream);
            }
            mFeatureSpecList.add(spec);
        }
    }

    public boolean isFeatureSupported(String tag) {
        Iterator<FeatureSpec> itr = mFeatureSpecList.iterator();
        while (itr.hasNext()) {
            FeatureSpec spec = itr.next();
            if (spec.mSpec.containsKey(tag)) {
                if (spec.mIsSpecOnStorage) {
                    LogHelper.d(TAG, "[isFeatureSupported] " + tag + " = " + spec.mSpec.get(tag) +
                            " from " + spec.mSpecName + " on storage");
                } else {
                    LogHelper.d(TAG, "[isFeatureSupported] " + tag + " = " + spec.mSpec.get(tag) +
                            " from " + spec.mSpecName + " in assets");
                }
                return spec.mSpec.get(tag);
            }
        }
        LogHelper.d(TAG, "[isFeatureSupported] " + tag + " = false by default");
        return false;
    }

    private String[] getPossibleSpecName() {
        String[] names = new String[5];
        String platform = SystemProperties.getString("ro.mediatek.platform", "null");
        String version = SystemProperties.getString("ro.mediatek.version.branch", "null");
        String flavor = SystemProperties.getString("ro.product.model", "null");
        String apiVersion = "null";

        switch (CameraApiHelper.getCameraApiType(null)) {
            case API1:
                apiVersion = "api1";
                break;
            case API2:
                apiVersion = "api2";
                break;
        }

        LogHelper.d(TAG, "[getPossibleSpecName] current api version " + apiVersion);

        int index = 0;
        names[index++] = SPEC_NAME_PREFIX
                + SPEC_NAME_SEPARATOR + apiVersion
                + SPEC_NAME_SEPARATOR + flavor
                + SPEC_NAME_SEPARATOR + version
                + SPEC_NAME_SUFFIX;

        names[index++] = SPEC_NAME_PREFIX
                + SPEC_NAME_SEPARATOR + apiVersion
                + SPEC_NAME_SEPARATOR + flavor
                + SPEC_NAME_SUFFIX;

        names[index++] = SPEC_NAME_PREFIX
                + SPEC_NAME_SEPARATOR + apiVersion
                + SPEC_NAME_SEPARATOR + platform
                + SPEC_NAME_SEPARATOR + version
                + SPEC_NAME_SUFFIX;

        names[index++] = SPEC_NAME_PREFIX
                + SPEC_NAME_SEPARATOR + apiVersion
                + SPEC_NAME_SEPARATOR + platform
                + SPEC_NAME_SUFFIX;

        names[index++] = SPEC_NAME_PREFIX
                + SPEC_NAME_SEPARATOR + apiVersion
                + SPEC_NAME_SUFFIX;

        for (String name : names) {
            LogHelper.d(TAG, "[getPossibleSpecName] possible spec file = " + name);
        }

        return names;
    }

    private InputStream getInputStream(String specFileName, FeatureSpec spec) {
        InputStream inputStream = getInputStreamFromStorage(specFileName);
        if (inputStream != null) {
            spec.mIsSpecOnStorage = true;
            return inputStream;
        } else {
            inputStream = getInputStreamFromAsset(specFileName);
        }
        return inputStream;
    }

    private InputStream getInputStreamFromStorage(final String specFileName) {
        File[] specFile = Environment.getExternalStorageDirectory()
                .listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.equals(specFileName);
                    }
                });
        if (specFile != null && specFile.length != 0 && specFile[0] != null) {
            try {
                LogHelper.d(TAG, "[getInputStreamFromStorage] Read spec file from storage "
                        + specFile[0].getAbsolutePath());
                return new FileInputStream(specFile[0]);
            } catch (IOException e) {
                LogHelper.d(TAG, "[getInputStreamFromStorage] IOException pop up", e);
                return null;
            }
        }
        return null;
    }

    private InputStream getInputStreamFromAsset(String specFileName) {
        String[] assetFiles = new String[0];
        try {
            assetFiles = mContext.getAssets().list("");
        } catch (IOException e) {
            LogHelper.d(TAG, "[getInputStreamFromAsset] IOException pop up", e);
        }

        String specFile = null;
        for (String file : assetFiles) {
            if (file.equals(specFileName)) {
                specFile = file;
                break;
            }
        }

        if (specFile == null) {
            return null;
        }

        try {
            LogHelper.d(TAG, "[getInputStreamFromAsset] Read spec file from assets " + specFile);
            return mContext.getAssets().open(specFile);
        } catch (IOException e) {
            LogHelper.d(TAG, "[getInputStreamFromAsset] IOException pop up", e);
            return null;
        }
    }

    private void initFeatureSpec(InputStream stream, FeatureSpec spec) {
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            inputStreamReader = new InputStreamReader(stream, "UTF-8");
            bufferedReader = new BufferedReader(inputStreamReader);

            String line;
            String[] keyValue;
            while ((line = bufferedReader.readLine()) != null) {
                keyValue = line.split(",");
                spec.mSpec.put(keyValue[0], Boolean.valueOf("true".equals(keyValue[1])));
            }
        } catch (UnsupportedEncodingException e) {
            LogHelper.d(TAG, "[initFeatureSpec] UnsupportedEncodingException pop up", e);
        } catch (IOException e) {
            LogHelper.d(TAG, "[initFeatureSpec] IOException pop up", e);
        } finally {
            closeSafely(bufferedReader);
            closeSafely(inputStreamReader);
        }
    }

    private void closeSafely(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
    }

    private void closeSafely(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }
}
