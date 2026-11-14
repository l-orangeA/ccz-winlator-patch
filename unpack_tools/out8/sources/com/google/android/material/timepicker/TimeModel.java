package com.google.android.material.timepicker;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class TimeModel implements Parcelable {
    public static final Parcelable.Creator<TimeModel> CREATOR = new Parcelable.Creator<TimeModel>() { // from class: com.google.android.material.timepicker.TimeModel.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeModel createFromParcel(Parcel in) {
            return new TimeModel(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TimeModel[] newArray(int size) {
            return new TimeModel[size];
        }
    };
    final int format;
    int hour;
    private final MaxInputValidator hourInputValidator;
    int minute;
    private final MaxInputValidator minuteInputValidator;
    int period;
    int selection;

    public TimeModel() {
        this(0);
    }

    public TimeModel(int format) {
        this(0, 0, 10, format);
    }

    public TimeModel(int hour, int minute, int selection, int format) {
        this.hour = hour;
        this.minute = minute;
        this.selection = selection;
        this.format = format;
        this.period = getPeriod(hour);
        this.minuteInputValidator = new MaxInputValidator(59);
        this.hourInputValidator = new MaxInputValidator(format == 1 ? 24 : 12);
    }

    protected TimeModel(Parcel in) {
        this(in.readInt(), in.readInt(), in.readInt(), in.readInt());
    }

    private static int getPeriod(int hourOfDay) {
        return hourOfDay >= 12 ? 1 : 0;
    }

    public int hashCode() {
        Object[] hashedFields = {Integer.valueOf(this.format), Integer.valueOf(this.hour), Integer.valueOf(this.minute), Integer.valueOf(this.selection)};
        return Arrays.hashCode(hashedFields);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimeModel)) {
            return false;
        }
        TimeModel that = (TimeModel) o;
        return this.hour == that.hour && this.minute == that.minute && this.format == that.format && this.selection == that.selection;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.hour);
        dest.writeInt(this.minute);
        dest.writeInt(this.selection);
        dest.writeInt(this.format);
    }

    public static String formatText(Resources resources, CharSequence text) {
        return formatText(resources, text, "%02d");
    }

    public static String formatText(Resources resources, CharSequence text, String format) {
        return String.format(resources.getConfiguration().locale, format, Integer.valueOf(Integer.parseInt(String.valueOf(text))));
    }
}
