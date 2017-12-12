package com.activis.jaycee.targetfinder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.RadioButton;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ActivityWelcome extends Activity
{
    private static final String TAG = ActivityWelcome.class.getSimpleName();

    private int selectedPitch = 0;

    private String PREF_FILE_NAME;
    private String pitchDistHigh;
    private String pitchDistLow;
    private String gainDistHigh;
    private String gainDistLow;
    private String pitchHigh;
    private String pitchLow;
    private String gainHigh;
    private String gainLow;
    private String vibration;
    private String distanceThreshold;
    private String voiceTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        PREF_FILE_NAME = getString(R.string.pref_file_name);
        pitchDistHigh = getString(R.string.pref_name_pitch_dist_high);
        pitchDistLow = getString(R.string.pref_name_pitch_dist_low);
        gainDistHigh = getString(R.string.pref_name_gain_dist_high);
        gainDistLow = getString(R.string.pref_name_gain_dist_low);
        pitchHigh = getString(R.string.pref_name_pitch_high);
        pitchLow = getString(R.string.pref_name_pitch_low);
        gainHigh = getString(R.string.pref_name_gain_high);
        gainLow = getString(R.string.pref_name_gain_low);
        vibration = getString(R.string.pref_name_vibration_delay);
        distanceThreshold = getString(R.string.pref_name_distance_threshold);
        voiceTimer = getString(R.string.pref_name_voice_timing);

        readXmlAndSetPrefs("fast-pitch-fast-gain-slow-vibrate-slow-voice");

        findViewById(R.id.button_start).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(ActivityWelcome.this, ActivityCamera.class));
            }
        });
    }

    public void onRadioClick(View view)
    {
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId())
        {
            case R.id.radio_high:
                if (checked)
                {
                    readXmlAndSetPrefs("fast-pitch-fast-gain-slow-vibrate-slow-voice");
                }
                break;
            case R.id.radio_med:
                if (checked)
                {
                    readXmlAndSetPrefs("med-pitch-med-gain-slow-vibrate-slow-voice");
                }
                break;
            case R.id.radio_low:
                if (checked)
                {
                    readXmlAndSetPrefs("slow-pitch-slow-gain-slow-vibrate-slow-voice");
                }
                break;
        }
    }

    public void readXmlAndSetPrefs(String configFile)
    {
        List<XmlParser.Entry> entries = null;

        try
        {
            InputStream stream = getAssets().open("configs.xml");

            XmlParser parser = new XmlParser();
            entries = parser.parse(stream, configFile);
            XmlParser.Entry entry = entries.get(0);

            SharedPreferences prefs = getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            if (entry.vibration != null)
            {
                editor.putInt(vibration, Integer.valueOf(entry.vibration));
            }

            if (entry.pitchLow != null)
            {
                editor.putFloat(pitchLow, Float.valueOf(entry.pitchLow));
            }

            if (entry.pitchHigh != null)
            {
                editor.putFloat(pitchHigh, Float.valueOf(entry.pitchHigh));
            }

            if (entry.gainHigh != null)
            {
                editor.putFloat(gainHigh, Float.valueOf(entry.gainHigh));
            }

            if (entry.gainLow != null)
            {
                editor.putFloat(gainLow, Float.valueOf(entry.gainLow));
            }

            if (entry.pitchDistanceHigh != null)
            {
                editor.putFloat(pitchDistHigh, Float.valueOf(entry.pitchDistanceHigh));
            }

            if (entry.pitchDistanceLow != null)
            {
                editor.putFloat(pitchDistLow, Float.valueOf(entry.pitchDistanceLow));
            }

            if (entry.gainDistanceHigh != null)
            {
                editor.putFloat(gainDistHigh, Float.valueOf(entry.gainDistanceHigh));
            }

            if (entry.gainDistanceLow != null)
            {
                editor.putFloat(gainDistLow, Float.valueOf(entry.gainDistanceLow));
            }

            if (entry.distanceThreshold != null)
            {
                editor.putFloat(distanceThreshold, Float.valueOf(entry.distanceThreshold));
            }

            if (entry.voiceTiming != null)
            {
                editor.putInt(voiceTimer, Integer.valueOf(entry.voiceTiming));
            }

            if (!editor.commit())
            {
                Log.e(TAG, "Error writing preferences");
            }
        }

        catch (IOException e)
        {
            Log.e(TAG, "File read error: ", e);
        }

        catch (XmlPullParserException e)
        {
            Log.e(TAG, "Xml Parse error: ", e);
        }
    }
}

class XmlParser
{
    private static final String TAG = XmlParser.class.getSimpleName();

    private static final String ns = null;

    public List parse(InputStream in, String configSelection) throws XmlPullParserException, IOException
    {
        try
        {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            return readConfigs(parser, configSelection);
        }

        finally
        {
            in.close();
        }
    }

    private List readConfigs(XmlPullParser parser, String configSelection) throws XmlPullParserException, IOException
    {
        List entries = new ArrayList();

        parser.require(XmlPullParser.START_TAG, ns, "configurations");
        while (parser.next() != XmlPullParser.END_TAG)
        {
            if (parser.getEventType() != XmlPullParser.START_TAG)
            {
                continue;
            }

            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals(configSelection))
            {
                entries.add(readEntry(parser, configSelection));
            }
            else
            {
                skip(parser);
            }
        }

        return entries;
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    private Entry readEntry(XmlPullParser parser, String configSelection) throws XmlPullParserException, IOException
    {
        parser.require(XmlPullParser.START_TAG, ns, configSelection);
        String pitchDistanceHigh = null;
        String pitchDistanceLow = null;
        String gainHigh = null;
        String gainLow = null;
        String gainDistanceHigh = null;
        String gainDistanceLow = null;
        String pitchHigh = null;
        String pitchLow = null;
        String vibration = null;
        String distanceThreshold = null;
        String voiceTiming = null;

        while (parser.next() != XmlPullParser.END_TAG)
        {
            if (parser.getEventType() != XmlPullParser.START_TAG)
            {
                continue;
            }

            String name = parser.getName();
            Log.d(TAG, name);
            if (name.equals("pitchDistanceHigh"))
            {
                pitchDistanceHigh = readDistance(parser, true);
            }
            else if (name.equals("pitchDistanceLow"))
            {
                pitchDistanceLow = readDistance(parser, false);
            }
            else if (name.equals("gainHigh"))
            {
                gainHigh = readGain(parser, true);
            }
            else if (name.equals("gainLow"))
            {
                gainLow = readGain(parser, false);
            }
            else if (name.equals("gainDistanceHigh"))
            {
                gainDistanceHigh = readObstacleDistance(parser, true);
            }
            else if (name.equals("gainDistanceLow"))
            {
                gainDistanceLow = readObstacleDistance(parser, false);
            }
            else if (name.equals("pitchHigh"))
            {
                pitchHigh = readPitch(parser, true);
            }
            else if (name.equals("pitchLow"))
            {
                pitchLow = readPitch(parser, false);
            }
            else if (name.equals("vibration"))
            {
                vibration = readVibration(parser);
            }
            else if (name.equals("distanceThreshold"))
            {
                distanceThreshold = readDistanceThreshold(parser);
            }
            else if (name.equals("voiceTiming"))
            {
                voiceTiming = readVoiceTiming(parser);
            }
            else
            {
                skip(parser);
            }
        }

        return new Entry(pitchDistanceHigh, pitchDistanceLow, gainHigh, gainLow, gainDistanceHigh, gainDistanceLow, pitchHigh, pitchLow, vibration, distanceThreshold, voiceTiming);
    }

    private String readDistanceThreshold(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        parser.require(XmlPullParser.START_TAG, ns, "distanceThreshold");
        String distanceThreshold = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "distanceThreshold");

        return distanceThreshold;
    }

    private String readVibration(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        parser.require(XmlPullParser.START_TAG, ns, "vibration");
        String vibration = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "vibration");

        return vibration;
    }

    private String readVoiceTiming(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        parser.require(XmlPullParser.START_TAG, ns, "voiceTiming");
        String voiceTiming = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "voiceTiming");

        return voiceTiming;
    }

    private String readPitch(XmlPullParser parser, boolean isHigh) throws IOException, XmlPullParserException
    {
        String pitch = null;

        if (isHigh)
        {
            parser.require(XmlPullParser.START_TAG, ns, "pitchHigh");
            pitch = readText(parser);
            parser.require(XmlPullParser.END_TAG, ns, "pitchHigh");
        } else
        {
            parser.require(XmlPullParser.START_TAG, ns, "pitchLow");
            pitch = readText(parser);
            parser.require(XmlPullParser.END_TAG, ns, "pitchLow");
        }

        return pitch;
    }

    private String readGain(XmlPullParser parser, boolean isHigh) throws IOException, XmlPullParserException
    {
        String gain = null;

        if (isHigh)
        {
            parser.require(XmlPullParser.START_TAG, ns, "gainHigh");
            gain = readText(parser);
            parser.require(XmlPullParser.END_TAG, ns, "gainHigh");
        } else
        {
            parser.require(XmlPullParser.START_TAG, ns, "gainLow");
            gain = readText(parser);
            parser.require(XmlPullParser.END_TAG, ns, "gainLow");
        }

        return gain;
    }

    private String readObstacleDistance(XmlPullParser parser, boolean isHigh) throws IOException, XmlPullParserException
    {
        String distance = null;

        if (isHigh)
        {
            parser.require(XmlPullParser.START_TAG, ns, "gainDistanceHigh");
            distance = readText(parser);
            parser.require(XmlPullParser.END_TAG, ns, "gainDistanceHigh");
        } else
        {
            parser.require(XmlPullParser.START_TAG, ns, "gainDistanceLow");
            distance = readText(parser);
            parser.require(XmlPullParser.END_TAG, ns, "gainDistanceLow");
        }

        return distance;
    }

    private String readDistance(XmlPullParser parser, boolean isHigh) throws IOException, XmlPullParserException
    {
        String distance = null;

        if (isHigh)
        {
            parser.require(XmlPullParser.START_TAG, ns, "pitchDistanceHigh");
            distance = readText(parser);
            parser.require(XmlPullParser.END_TAG, ns, "pitchDistanceHigh");
        } else
        {
            parser.require(XmlPullParser.START_TAG, ns, "pitchDistanceLow");
            distance = readText(parser);
            parser.require(XmlPullParser.END_TAG, ns, "pitchDistanceLow");
        }

        return distance;
    }

    // For the tags title and summary, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT)
        {
            result = parser.getText();
            parser.nextTag();
        }

        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        if (parser.getEventType() != XmlPullParser.START_TAG)
        {
            throw new IllegalStateException();
        }

        int depth = 1;

        while (depth != 0)
        {
            switch (parser.next())
            {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    public static class Entry
    {
        public final String pitchDistanceHigh, pitchDistanceLow;
        public final String gainHigh, gainLow;
        public final String gainDistanceHigh, gainDistanceLow;
        public final String pitchHigh, pitchLow;
        public final String vibration;
        public final String distanceThreshold;
        public final String voiceTiming;

        private Entry(String pitchDistanceHigh, String pitchDistanceLow, String gainHigh, String gainLow, String gainDistanceHigh, String gainDistanceLow, String pitchHigh, String pitchLow, String vibration, String distanceThreshold, String voiceTiming)
        {
            this.pitchDistanceHigh = pitchDistanceHigh;
            this.pitchDistanceLow = pitchDistanceLow;
            this.gainHigh = gainHigh;
            this.gainLow = gainLow;
            this.gainDistanceHigh = gainDistanceHigh;
            this.gainDistanceLow = gainDistanceLow;
            this.pitchHigh = pitchHigh;
            this.pitchLow = pitchLow;
            this.vibration = vibration;
            this.distanceThreshold = distanceThreshold;
            this.voiceTiming = voiceTiming;
        }
    }
}