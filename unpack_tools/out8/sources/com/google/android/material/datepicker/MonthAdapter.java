package com.google.android.material.datepicker;

import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.Collection;
import java.util.Iterator;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class MonthAdapter extends BaseAdapter {
    static final int MAXIMUM_WEEKS = UtcDates.getUtcCalendar().getMaximum(4);
    final CalendarConstraints calendarConstraints;
    CalendarStyle calendarStyle;
    final DateSelector<?> dateSelector;
    final Month month;
    private Collection<Long> previouslySelectedDates;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MonthAdapter(Month month, DateSelector<?> dateSelector, CalendarConstraints calendarConstraints) {
        this.month = month;
        this.dateSelector = dateSelector;
        this.calendarConstraints = calendarConstraints;
        this.previouslySelectedDates = dateSelector.getSelectedDays();
    }

    @Override // android.widget.BaseAdapter, android.widget.Adapter
    public boolean hasStableIds() {
        return true;
    }

    @Override // android.widget.Adapter
    public Long getItem(int position) {
        if (position < this.month.daysFromStartOfWeekToFirstOfMonth() || position > lastPositionInMonth()) {
            return null;
        }
        return Long.valueOf(this.month.getDay(positionToDay(position)));
    }

    @Override // android.widget.Adapter
    public long getItemId(int position) {
        return position / this.month.daysInWeek;
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.month.daysInMonth + firstPositionInMonth();
    }

    /* JADX WARN: Removed duplicated region for block: B:14:0x0084 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:16:0x0085  */
    @Override // android.widget.Adapter
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public android.widget.TextView getView(int r11, android.view.View r12, android.view.ViewGroup r13) {
        /*
            r10 = this;
            android.content.Context r0 = r13.getContext()
            r10.initializeStyles(r0)
            r0 = r12
            android.widget.TextView r0 = (android.widget.TextView) r0
            r1 = 0
            if (r12 != 0) goto L1e
            android.content.Context r2 = r13.getContext()
            android.view.LayoutInflater r2 = android.view.LayoutInflater.from(r2)
            int r3 = com.google.android.material.R$layout.mtrl_calendar_day
            android.view.View r3 = r2.inflate(r3, r13, r1)
            r0 = r3
            android.widget.TextView r0 = (android.widget.TextView) r0
        L1e:
            int r2 = r10.firstPositionInMonth()
            int r2 = r11 - r2
            if (r2 < 0) goto L76
            com.google.android.material.datepicker.Month r3 = r10.month
            int r4 = r3.daysInMonth
            if (r2 < r4) goto L2d
            goto L76
        L2d:
            int r4 = r2 + 1
            r0.setTag(r3)
            android.content.res.Resources r3 = r0.getResources()
            android.content.res.Configuration r3 = r3.getConfiguration()
            java.util.Locale r3 = r3.locale
            r5 = 1
            java.lang.Object[] r6 = new java.lang.Object[r5]
            java.lang.Integer r7 = java.lang.Integer.valueOf(r4)
            r6[r1] = r7
            java.lang.String r7 = "%d"
            java.lang.String r6 = java.lang.String.format(r3, r7, r6)
            r0.setText(r6)
            com.google.android.material.datepicker.Month r6 = r10.month
            long r6 = r6.getDay(r4)
            com.google.android.material.datepicker.Month r8 = r10.month
            int r8 = r8.year
            com.google.android.material.datepicker.Month r9 = com.google.android.material.datepicker.Month.current()
            int r9 = r9.year
            if (r8 != r9) goto L68
            java.lang.String r8 = com.google.android.material.datepicker.DateStrings.getMonthDayOfWeekDay(r6)
            r0.setContentDescription(r8)
            goto L6f
        L68:
            java.lang.String r8 = com.google.android.material.datepicker.DateStrings.getYearMonthDayOfWeekDay(r6)
            r0.setContentDescription(r8)
        L6f:
            r0.setVisibility(r1)
            r0.setEnabled(r5)
            goto L7e
        L76:
            r3 = 8
            r0.setVisibility(r3)
            r0.setEnabled(r1)
        L7e:
            java.lang.Long r1 = r10.getItem(r11)
            if (r1 != 0) goto L85
            return r0
        L85:
            long r3 = r1.longValue()
            r10.updateSelectedState(r0, r3)
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.android.material.datepicker.MonthAdapter.getView(int, android.view.View, android.view.ViewGroup):android.widget.TextView");
    }

    public void updateSelectedStates(MaterialCalendarGridView monthGrid) {
        for (Long date : this.previouslySelectedDates) {
            updateSelectedStateForDate(monthGrid, date.longValue());
        }
        DateSelector<?> dateSelector = this.dateSelector;
        if (dateSelector != null) {
            for (Long date2 : dateSelector.getSelectedDays()) {
                updateSelectedStateForDate(monthGrid, date2.longValue());
            }
            this.previouslySelectedDates = this.dateSelector.getSelectedDays();
        }
    }

    private void updateSelectedStateForDate(MaterialCalendarGridView monthGrid, long date) {
        if (Month.create(date).equals(this.month)) {
            int day = this.month.getDayOfMonth(date);
            updateSelectedState((TextView) monthGrid.getChildAt(monthGrid.getAdapter().dayToPosition(day) - monthGrid.getFirstVisiblePosition()), date);
        }
    }

    private void updateSelectedState(TextView day, long date) {
        CalendarItemStyle style;
        if (day == null) {
            return;
        }
        if (this.calendarConstraints.getDateValidator().isValid(date)) {
            day.setEnabled(true);
            if (isSelected(date)) {
                style = this.calendarStyle.selectedDay;
            } else if (UtcDates.getTodayCalendar().getTimeInMillis() == date) {
                style = this.calendarStyle.todayDay;
            } else {
                style = this.calendarStyle.day;
            }
        } else {
            day.setEnabled(false);
            style = this.calendarStyle.invalidDay;
        }
        style.styleItem(day);
    }

    private boolean isSelected(long date) {
        Iterator<Long> it = this.dateSelector.getSelectedDays().iterator();
        while (it.hasNext()) {
            long selectedDay = it.next().longValue();
            if (UtcDates.canonicalYearMonthDay(date) == UtcDates.canonicalYearMonthDay(selectedDay)) {
                return true;
            }
        }
        return false;
    }

    private void initializeStyles(Context context) {
        if (this.calendarStyle == null) {
            this.calendarStyle = new CalendarStyle(context);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int firstPositionInMonth() {
        return this.month.daysFromStartOfWeekToFirstOfMonth();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int lastPositionInMonth() {
        return (this.month.daysFromStartOfWeekToFirstOfMonth() + this.month.daysInMonth) - 1;
    }

    int positionToDay(int position) {
        return (position - this.month.daysFromStartOfWeekToFirstOfMonth()) + 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int dayToPosition(int day) {
        int offsetFromFirst = day - 1;
        return firstPositionInMonth() + offsetFromFirst;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean withinMonth(int position) {
        return position >= firstPositionInMonth() && position <= lastPositionInMonth();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFirstInRow(int position) {
        return position % this.month.daysInWeek == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLastInRow(int position) {
        return (position + 1) % this.month.daysInWeek == 0;
    }
}
