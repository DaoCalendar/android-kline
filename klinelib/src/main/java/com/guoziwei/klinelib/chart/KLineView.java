package com.guoziwei.klinelib.chart;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ICandleDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Transformer;
import com.guoziwei.klinelib.R;
import com.guoziwei.klinelib.model.HisData;
import com.guoziwei.klinelib.util.DataUtils;
import com.guoziwei.klinelib.util.DateUtils;
import com.guoziwei.klinelib.util.DisplayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * kline
 * Created by guoziwei on 2017/10/26.
 */
public class KLineView extends LinearLayout {


    public static final int NORMAL_LINE = 0;
    /**
     * average line
     */
    public static final int AVE_LINE = 1;
    /**
     * hide line
     */
    public static final int INVISIABLE_LINE = 6;


    public static final int MA5 = 5;
    public static final int MA10 = 10;
    public static final int MA20 = 20;
    public static final int MA30 = 30;

    public static final int K = 31;
    public static final int D = 32;
    public static final int J = 33;

    public int MAX_COUNT_LINE = 300;
    public int MIN_COUNT_LINE = 50;
    public int MAX_COUNT_K = 300;
    public int MIN_COUNT_K = 30;

    protected AppCombinedChart mChartPrice;
    protected AppCombinedChart mChartVolume;
    protected AppCombinedChart mChartMacd;
    protected AppCombinedChart mChartKdj;
    protected List<HisData> mData = new ArrayList<>(300);

    protected ChartInfoView mChartInfoView;
    protected Context mContext;
    private int mAxisColor;
    private int mTransparentColor;

    /**
     * last price
     */
    private double mLastPrice;

    /**
     * yesterday close price
     */
    private double mLastClose;

    /**
     * the digits of the symbol
     */
    private int mDigits = 2;
    private int mDecreasingColor;
    private int mIncreasingColor;

    public KLineView(Context context) {
        this(context, null);
    }

    public KLineView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KLineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.view_kline, this);
        mChartPrice = (AppCombinedChart) findViewById(R.id.price_chart);
        mChartVolume = (AppCombinedChart) findViewById(R.id.vol_chart);
        mChartMacd = (AppCombinedChart) findViewById(R.id.macd_chart);
        mChartKdj = (AppCombinedChart) findViewById(R.id.kdj_chart);
        mChartInfoView = (ChartInfoView) findViewById(R.id.k_info);
        RadioGroup rgIndex = findViewById(R.id.rg_index);
        rgIndex.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.cb_vol) {
                    mChartMacd.setVisibility(GONE);
                    mChartKdj.setVisibility(GONE);
                    mChartVolume.setVisibility(VISIBLE);
                } else if (checkedId == R.id.cb_macd) {
                    mChartVolume.setVisibility(GONE);
                    mChartMacd.setVisibility(VISIBLE);
                    mChartKdj.setVisibility(GONE);
                } else if (checkedId == R.id.cb_kdj) {
                    mChartVolume.setVisibility(GONE);
                    mChartMacd.setVisibility(GONE);
                    mChartKdj.setVisibility(VISIBLE);
                }
            }
        });
        ((RadioButton) rgIndex.getChildAt(0)).setChecked(true);
        mChartInfoView.setChart(mChartPrice, mChartVolume, mChartMacd, mChartKdj);
        mAxisColor = ContextCompat.getColor(mContext, R.color.axis_color);
        mTransparentColor = getResources().getColor(android.R.color.transparent);
        mDecreasingColor = ContextCompat.getColor(getContext(), R.color.decreasing_color);
        mIncreasingColor = ContextCompat.getColor(getContext(), R.color.increasing_color);
        mChartPrice.setNoDataText(context.getString(R.string.loading));
        initChartPrice();
        initBottomChart(mChartVolume);
        initBottomChart(mChartMacd);
        initBottomChart(mChartKdj);
        setOffset();
        initChartListener();
    }


    protected void initChartPrice() {
        mChartPrice.setScaleEnabled(true);
        mChartPrice.setDrawBorders(false);
        mChartPrice.setBorderWidth(1);
        mChartPrice.setDragEnabled(true);
        mChartPrice.setScaleYEnabled(false);
        mChartPrice.getDescription().setEnabled(false);
        mChartPrice.setAutoScaleMinMaxEnabled(true);
        mChartPrice.setDragDecelerationEnabled(false);
        LineChartXMarkerView mvx = new LineChartXMarkerView(mContext, mData);
        mvx.setChartView(mChartPrice);
        mChartPrice.setXMarker(mvx);
        Legend lineChartLegend = mChartPrice.getLegend();
        lineChartLegend.setEnabled(false);

        XAxis xAxisPrice = mChartPrice.getXAxis();
        xAxisPrice.setDrawLabels(false);
        xAxisPrice.setDrawAxisLine(false);
        xAxisPrice.setDrawGridLines(false);
        xAxisPrice.setAxisMinimum(-0.5f);


        YAxis axisLeftPrice = mChartPrice.getAxisLeft();
        axisLeftPrice.setLabelCount(5, true);
        axisLeftPrice.setDrawLabels(true);
        axisLeftPrice.setDrawGridLines(false);

        axisLeftPrice.setDrawAxisLine(false);
        axisLeftPrice.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        axisLeftPrice.setTextColor(mAxisColor);
        axisLeftPrice.setValueFormatter(new YValueFormatter(mDigits));

        int[] colorArray = {mDecreasingColor, mDecreasingColor, mAxisColor, mIncreasingColor, mIncreasingColor};
        Transformer leftYTransformer = mChartPrice.getRendererLeftYAxis().getTransformer();
        ColorContentYAxisRenderer leftColorContentYAxisRenderer = new ColorContentYAxisRenderer(mChartPrice.getViewPortHandler(), mChartPrice.getAxisLeft(), leftYTransformer);
        leftColorContentYAxisRenderer.setLabelColor(colorArray);
        leftColorContentYAxisRenderer.setLabelInContent(true);
        leftColorContentYAxisRenderer.setUseDefaultLabelXOffset(false);
        mChartPrice.setRendererLeftYAxis(leftColorContentYAxisRenderer);


        YAxis axisRightPrice = mChartPrice.getAxisRight();
        axisRightPrice.setLabelCount(5, true);
        axisRightPrice.setDrawLabels(true);
        axisRightPrice.setDrawGridLines(false);
        axisRightPrice.setDrawAxisLine(false);
        axisRightPrice.setTextColor(mAxisColor);
        axisRightPrice.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        axisRightPrice.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                double rate = (value - mLastClose) / mLastClose * 100;
                if (Double.isNaN(rate) || Double.isInfinite(rate)) {
                    return "";
                }
                String s = String.format(Locale.getDefault(), "%.2f%%",
                        rate);
                if (TextUtils.equals("-0.00%", s)) {
                    return "0.00%";
                }
                return s;
            }
        });

//        设置标签Y渲染器
        Transformer rightYTransformer = mChartPrice.getRendererRightYAxis().getTransformer();
        ColorContentYAxisRenderer rightColorContentYAxisRenderer = new ColorContentYAxisRenderer(mChartPrice.getViewPortHandler(), mChartPrice.getAxisRight(), rightYTransformer);
        rightColorContentYAxisRenderer.setLabelInContent(true);
        rightColorContentYAxisRenderer.setUseDefaultLabelXOffset(false);
        rightColorContentYAxisRenderer.setLabelColor(colorArray);
        mChartPrice.setRendererRightYAxis(rightColorContentYAxisRenderer);

    }


    protected void initBottomChart(AppCombinedChart chart) {
        chart.setScaleEnabled(true);
        chart.setDrawBorders(false);
        chart.setBorderWidth(1);
        chart.setDragEnabled(true);
        chart.setScaleYEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.setAutoScaleMinMaxEnabled(true);
        chart.setDragDecelerationEnabled(false);
        chart.setHighlightPerDragEnabled(false);
        Legend lineChartLegend = chart.getLegend();
        lineChartLegend.setEnabled(false);


        XAxis xAxisVolume = chart.getXAxis();
        xAxisVolume.setDrawLabels(true);
        xAxisVolume.setDrawAxisLine(false);
        xAxisVolume.setDrawGridLines(false);
        xAxisVolume.setTextColor(mAxisColor);
        xAxisVolume.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisVolume.setLabelCount(5, true);
        xAxisVolume.setAvoidFirstLastClipping(true);
        xAxisVolume.setAxisMinimum(-0.5f);

        xAxisVolume.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if (value < 0) {
                    value = 0;
                }
                if (mData != null && value < mData.size() && value >= 0) {
                    return DateUtils.formatTime(mData.get((int) value).getDate());
                }
                return "";
            }
        });

        YAxis axisLeftVolume = chart.getAxisLeft();
        axisLeftVolume.setDrawLabels(true);
        axisLeftVolume.setDrawGridLines(false);
        axisLeftVolume.setLabelCount(3, true);
        axisLeftVolume.setDrawAxisLine(false);
        axisLeftVolume.setTextColor(mAxisColor);
        axisLeftVolume.setSpaceTop(0);
        axisLeftVolume.setSpaceBottom(0);
        axisLeftVolume.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        /*axisLeftVolume.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                String s;
                if (value > 10000) {
                    s = (int) (value / 10000) + "w";
                } else if (value > 1000) {
                    s = (int) (value / 1000) + "k";
                } else {
                    s = (int) value + "";
                }
                return String.format(Locale.getDefault(), "%1$5s", s);
            }
        });
*/
        Transformer leftYTransformer = chart.getRendererLeftYAxis().getTransformer();
        ColorContentYAxisRenderer leftColorContentYAxisRenderer = new ColorContentYAxisRenderer(chart.getViewPortHandler(), chart.getAxisLeft(), leftYTransformer);
        leftColorContentYAxisRenderer.setLabelInContent(true);
        leftColorContentYAxisRenderer.setUseDefaultLabelXOffset(false);
        chart.setRendererLeftYAxis(leftColorContentYAxisRenderer);

        //右边y
        YAxis axisRightVolume = chart.getAxisRight();
        axisRightVolume.setDrawLabels(false);
        axisRightVolume.setDrawGridLines(false);
        axisRightVolume.setDrawAxisLine(false);

    }


    private void initChartListener() {
        mChartPrice.setOnChartGestureListener(new CoupleChartGestureListener(mChartPrice, mChartVolume, mChartMacd, mChartKdj));
        mChartVolume.setOnChartGestureListener(new CoupleChartGestureListener(mChartVolume, mChartPrice, mChartMacd, mChartKdj));
        mChartMacd.setOnChartGestureListener(new CoupleChartGestureListener(mChartMacd, mChartPrice, mChartVolume, mChartKdj));
        mChartKdj.setOnChartGestureListener(new CoupleChartGestureListener(mChartKdj, mChartPrice, mChartVolume, mChartMacd));
        mChartPrice.setOnChartValueSelectedListener(new InfoViewListener(mContext, mLastClose, mData, mChartInfoView, mChartVolume, mChartMacd, mChartKdj));
        mChartVolume.setOnChartValueSelectedListener(new InfoViewListener(mContext, mLastClose, mData, mChartInfoView, mChartPrice, mChartMacd, mChartKdj));
        mChartMacd.setOnChartValueSelectedListener(new InfoViewListener(mContext, mLastClose, mData, mChartInfoView, mChartPrice, mChartVolume, mChartKdj));
        mChartKdj.setOnChartValueSelectedListener(new InfoViewListener(mContext, mLastClose, mData, mChartInfoView, mChartPrice, mChartVolume, mChartMacd));
        mChartPrice.setOnTouchListener(new ChartInfoViewHandler(mChartPrice));
        mChartVolume.setOnTouchListener(new ChartInfoViewHandler(mChartVolume));
        mChartMacd.setOnTouchListener(new ChartInfoViewHandler(mChartMacd));
        mChartKdj.setOnTouchListener(new ChartInfoViewHandler(mChartKdj));
    }

    public void initChartKData(List<HisData> hisDatas) {
        mData.clear();
        mData.addAll(DataUtils.calculateHisData(hisDatas));

        ArrayList<CandleEntry> lineCJEntries = new ArrayList<>(MAX_COUNT_K);
        ArrayList<Entry> ma5Entries = new ArrayList<>(MAX_COUNT_K);
        ArrayList<Entry> ma10Entries = new ArrayList<>(MAX_COUNT_K);
        ArrayList<Entry> ma20Entries = new ArrayList<>(MAX_COUNT_K);
        ArrayList<Entry> ma30Entries = new ArrayList<>(MAX_COUNT_K);
        ArrayList<Entry> paddingEntries = new ArrayList<>(MAX_COUNT_K);

        for (int i = 0; i < mData.size(); i++) {
            HisData hisData = mData.get(i);
            lineCJEntries.add(new CandleEntry(i, (float) hisData.getHigh(), (float) hisData.getLow(), (float) hisData.getOpen(), (float) hisData.getClose()));

            if (!Double.isNaN(hisData.getMa5())) {
                ma5Entries.add(new Entry(i, (float) hisData.getMa5()));
            }

            if (!Double.isNaN(hisData.getMa10())) {
                ma10Entries.add(new Entry(i, (float) hisData.getMa10()));
            }

            if (!Double.isNaN(hisData.getMa20())) {
                ma20Entries.add(new Entry(i, (float) hisData.getMa20()));
            }

            if (!Double.isNaN(hisData.getMa30())) {
                ma30Entries.add(new Entry(i, (float) hisData.getMa30()));
            }
        }

        if (!mData.isEmpty() && mData.size() < MAX_COUNT_K) {
            for (int i = mData.size(); i < MAX_COUNT_K; i++) {
                paddingEntries.add(new Entry(i, (float) mData.get(mData.size() - 1).getClose()));
            }
        }

        LineData lineData = new LineData(
                setLine(INVISIABLE_LINE, paddingEntries),
                setLine(MA5, ma5Entries),
                setLine(MA10, ma10Entries),
                setLine(MA20, ma20Entries),
                setLine(MA30, ma30Entries));
        CandleData candleData = new CandleData(setKLine(NORMAL_LINE, lineCJEntries));
        CombinedData combinedData = new CombinedData();
        combinedData.setData(lineData);
        combinedData.setData(candleData);
        mChartPrice.setData(combinedData);

        mChartPrice.setVisibleXRange(MAX_COUNT_K, MIN_COUNT_K);
        mChartPrice.notifyDataSetChanged();
        mChartPrice.invalidate();
        mChartPrice.moveViewToX(combinedData.getEntryCount());
        initChartVolumeData();
        initChartMacdData();
        initChartKdjData();

        mChartVolume.zoom(3, 0, mData.size() / 2, 0);
        mChartPrice.zoom(3, 0, mData.size() / 2, 0);
        mChartMacd.zoom(3, 0, mData.size() / 2, 0);
        mChartKdj.zoom(3, 0, mData.size() / 2, 0);
    }

    public void initChartPriceData(List<HisData> hisDatas) {

        mData.clear();
        mData.addAll(DataUtils.calculateHisData(hisDatas));

        ArrayList<Entry> lineCJEntries = new ArrayList<>(MAX_COUNT_LINE);
        ArrayList<Entry> lineJJEntries = new ArrayList<>(MAX_COUNT_LINE);
        ArrayList<Entry> paddingEntries = new ArrayList<>(MAX_COUNT_LINE);

        for (int i = 0; i < mData.size(); i++) {
            lineCJEntries.add(new Entry(i, (float) mData.get(i).getClose()));
            lineJJEntries.add(new Entry(i, (float) mData.get(i).getAvePrice()));
        }
        if (!mData.isEmpty() && mData.size() < MAX_COUNT_LINE) {
            for (int i = mData.size(); i < MAX_COUNT_LINE; i++) {
                paddingEntries.add(new Entry(i, (float) mData.get(mData.size() - 1).getClose()));
            }
        }
        ArrayList<ILineDataSet> sets = new ArrayList<>();
        sets.add(setLine(NORMAL_LINE, lineCJEntries));
        sets.add(setLine(AVE_LINE, lineJJEntries));
        sets.add(setLine(INVISIABLE_LINE, paddingEntries));
        LineData lineData = new LineData(sets);

        CombinedData combinedData = new CombinedData();
        combinedData.setData(lineData);
        mChartPrice.setData(combinedData);

        mChartPrice.setVisibleXRange(MAX_COUNT_LINE, MIN_COUNT_LINE);

        mChartPrice.notifyDataSetChanged();
        mChartPrice.invalidate();
        mChartPrice.moveViewToX(combinedData.getEntryCount());
        initChartVolumeData();
    }


    private BarDataSet setBar(ArrayList<BarEntry> barEntries, int type) {
        BarDataSet barDataSet = new BarDataSet(barEntries, "vol");
        barDataSet.setHighLightAlpha(120);
        barDataSet.setHighLightColor(getResources().getColor(R.color.highlight_color));
        barDataSet.setDrawValues(false);
        barDataSet.setVisible(type != INVISIABLE_LINE);
        barDataSet.setHighlightEnabled(type != INVISIABLE_LINE);
        barDataSet.setColors(getResources().getColor(R.color.increasing_color), getResources().getColor(R.color.decreasing_color));
        return barDataSet;
    }


    @android.support.annotation.NonNull
    private LineDataSet setLine(int type, ArrayList<Entry> lineEntries) {
        LineDataSet lineDataSetMa = new LineDataSet(lineEntries, "ma" + type);
        lineDataSetMa.setDrawValues(false);
        if (type == NORMAL_LINE) {
            lineDataSetMa.setColor(getResources().getColor(R.color.normal_line_color));
            lineDataSetMa.setCircleColor(ContextCompat.getColor(mContext, R.color.normal_line_color));
        } else if (type == K) {
            lineDataSetMa.setColor(getResources().getColor(R.color.ma5));
            lineDataSetMa.setCircleColor(mTransparentColor);
        } else if (type == D) {
            lineDataSetMa.setColor(getResources().getColor(R.color.ma10));
            lineDataSetMa.setCircleColor(mTransparentColor);
        } else if (type == J) {
            lineDataSetMa.setColor(getResources().getColor(R.color.ma20));
            lineDataSetMa.setCircleColor(mTransparentColor);
        } else if (type == AVE_LINE) {
            lineDataSetMa.setColor(getResources().getColor(R.color.ave_color));
            lineDataSetMa.setCircleColor(mTransparentColor);
            lineDataSetMa.setHighlightEnabled(false);
        } else if (type == MA5) {
            lineDataSetMa.setColor(getResources().getColor(R.color.ma5));
            lineDataSetMa.setCircleColor(mTransparentColor);
            lineDataSetMa.setHighlightEnabled(false);
        } else if (type == MA10) {
            lineDataSetMa.setColor(getResources().getColor(R.color.ma10));
            lineDataSetMa.setCircleColor(mTransparentColor);
            lineDataSetMa.setHighlightEnabled(false);
        } else if (type == MA20) {
            lineDataSetMa.setColor(getResources().getColor(R.color.ma20));
            lineDataSetMa.setCircleColor(mTransparentColor);
            lineDataSetMa.setHighlightEnabled(false);
        } else if (type == MA30) {
            lineDataSetMa.setColor(getResources().getColor(R.color.ma30));
            lineDataSetMa.setCircleColor(mTransparentColor);
            lineDataSetMa.setHighlightEnabled(false);
        } else {
            lineDataSetMa.setVisible(false);
            lineDataSetMa.setHighlightEnabled(false);
        }
        lineDataSetMa.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSetMa.setLineWidth(1f);
        lineDataSetMa.setCircleRadius(1f);

        lineDataSetMa.setDrawCircles(false);
        lineDataSetMa.setDrawCircleHole(false);

        return lineDataSetMa;
    }

    @android.support.annotation.NonNull
    public CandleDataSet setKLine(int type, ArrayList<CandleEntry> lineEntries) {
        CandleDataSet set1 = new CandleDataSet(lineEntries, "KLine" + type);
        set1.setDrawIcons(false);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setShadowColor(Color.DKGRAY);
        set1.setShadowWidth(0.7f);
        set1.setDecreasingColor(mDecreasingColor);
        set1.setDecreasingPaintStyle(Paint.Style.FILL);
        set1.setShadowColorSameAsCandle(true);
        set1.setIncreasingColor(mIncreasingColor);
        set1.setIncreasingPaintStyle(Paint.Style.FILL);
        set1.setNeutralColor(ContextCompat.getColor(getContext(), R.color.increasing_color));
        set1.setDrawValues(false);
        set1.setHighlightEnabled(true);
        if (type != NORMAL_LINE) {
            set1.setVisible(false);
        }
        return set1;
    }

    private void initChartVolumeData() {
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<BarEntry> paddingEntries = new ArrayList<>();
        for (int i = 0; i < mData.size(); i++) {
            HisData t = mData.get(i);
            barEntries.add(new BarEntry(i, (float) t.getVol(), t));
        }
        int maxCount = mChartPrice.getData().getCandleData() == null ? MAX_COUNT_LINE : MAX_COUNT_K;
        if (!mData.isEmpty() && mData.size() < maxCount) {
            for (int i = mData.size(); i < maxCount; i++) {
                paddingEntries.add(new BarEntry(i, 0));
            }
        }

        BarData barData = new BarData(setBar(barEntries, NORMAL_LINE), setBar(paddingEntries, INVISIABLE_LINE));
        barData.setBarWidth(0.75f);
        CombinedData combinedData = new CombinedData();
        combinedData.setData(barData);
        mChartVolume.setData(combinedData);

        if (mChartPrice.getData().getCandleData() != null) {
            mChartVolume.setVisibleXRange(MAX_COUNT_K, MIN_COUNT_K);
        } else {
            mChartVolume.setVisibleXRange(MAX_COUNT_LINE, MIN_COUNT_LINE);
        }

//        setOffset();
        mChartVolume.notifyDataSetChanged();
        mChartVolume.invalidate();
        mChartVolume.moveViewToX(combinedData.getEntryCount());

    }

    private void initChartMacdData() {
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<BarEntry> paddingEntries = new ArrayList<>();
        ArrayList<Entry> difEntries = new ArrayList<>();
        ArrayList<Entry> deaEntries = new ArrayList<>();
        for (int i = 0; i < mData.size(); i++) {
            HisData t = mData.get(i);
            barEntries.add(new BarEntry(i, (float) t.getMacd()));
            difEntries.add(new Entry(i, (float) t.getDif()));
            deaEntries.add(new Entry(i, (float) t.getDea()));
        }
        int maxCount = mChartPrice.getData().getCandleData() == null ? MAX_COUNT_LINE : MAX_COUNT_K;
        if (!mData.isEmpty() && mData.size() < maxCount) {
            for (int i = mData.size(); i < maxCount; i++) {
                paddingEntries.add(new BarEntry(i, 0));
            }
        }

        BarData barData = new BarData(setBar(barEntries, NORMAL_LINE), setBar(paddingEntries, INVISIABLE_LINE));
        barData.setBarWidth(0.75f);
        CombinedData combinedData = new CombinedData();
        combinedData.setData(barData);
        LineData lineData = new LineData(setLine(MA5, difEntries), setLine(MA10, deaEntries));
        combinedData.setData(lineData);
        mChartMacd.setData(combinedData);

        if (mChartPrice.getData().getCandleData() != null) {
            mChartMacd.setVisibleXRange(MAX_COUNT_K, MIN_COUNT_K);
        } else {
            mChartMacd.setVisibleXRange(MAX_COUNT_LINE, MIN_COUNT_LINE);
        }

//        setOffset();
        mChartMacd.notifyDataSetChanged();
        mChartMacd.invalidate();
        mChartMacd.moveViewToX(combinedData.getEntryCount());
    }

    private void initChartKdjData() {
        ArrayList<Entry> kEntries = new ArrayList<>(MAX_COUNT_LINE);
        ArrayList<Entry> dEntries = new ArrayList<>(MAX_COUNT_LINE);
        ArrayList<Entry> jEntries = new ArrayList<>(MAX_COUNT_LINE);
        ArrayList<Entry> paddingEntries = new ArrayList<>(MAX_COUNT_LINE);

        for (int i = 0; i < mData.size(); i++) {
            kEntries.add(new Entry(i, (float) mData.get(i).getK()));
            dEntries.add(new Entry(i, (float) mData.get(i).getD()));
            jEntries.add(new Entry(i, (float) mData.get(i).getJ()));
        }
        if (!mData.isEmpty() && mData.size() < MAX_COUNT_LINE) {
            for (int i = mData.size(); i < MAX_COUNT_LINE; i++) {
                paddingEntries.add(new Entry(i, (float) mData.get(mData.size() - 1).getK()));
            }
        }
        ArrayList<ILineDataSet> sets = new ArrayList<>();
        sets.add(setLine(K, kEntries));
        sets.add(setLine(D, dEntries));
        sets.add(setLine(J, jEntries));
        sets.add(setLine(INVISIABLE_LINE, paddingEntries));
        LineData lineData = new LineData(sets);

        CombinedData combinedData = new CombinedData();
        combinedData.setData(lineData);
        mChartKdj.setData(combinedData);

        if (mChartPrice.getData().getCandleData() != null) {
            mChartMacd.setVisibleXRange(MAX_COUNT_K, MIN_COUNT_K);
        } else {
            mChartMacd.setVisibleXRange(MAX_COUNT_LINE, MIN_COUNT_LINE);
        }

        mChartKdj.notifyDataSetChanged();
        mChartKdj.invalidate();
        mChartKdj.moveViewToX(combinedData.getEntryCount());
    }

    /**
     * according to the price to refresh the last data of the chart
     */
    public void refreshData(float price) {
        if (price <= 0 || price == mLastPrice) {
            return;
        }
        mLastPrice = price;
        CombinedData data = mChartPrice.getData();
        if (data == null) return;
        LineData lineData = data.getLineData();
        if (lineData != null) {
            ILineDataSet set = lineData.getDataSetByIndex(0);
            if (set.removeLast()) {
                set.addEntry(new Entry(set.getEntryCount(), price));
            }
        }
        CandleData candleData = data.getCandleData();
        if (candleData != null) {
            ICandleDataSet set = candleData.getDataSetByIndex(0);
            if (set.removeLast()) {
                HisData hisData = mData.get(mData.size() - 1);
                hisData.setClose(price);
                hisData.setHigh(Math.max(hisData.getHigh(), price));
                hisData.setLow(Math.min(hisData.getLow(), price));
                set.addEntry(new CandleEntry(set.getEntryCount(), (float) hisData.getHigh(), (float) hisData.getLow(), (float) hisData.getOpen(), price));
            }
        }
        mChartPrice.notifyDataSetChanged();
        mChartPrice.invalidate();
    }


    public void addKData(HisData hisData) {
        hisData = DataUtils.calculateHisData(hisData, mData);
        CombinedData combinedData = mChartPrice.getData();
        LineData priceData = combinedData.getLineData();
        ILineDataSet ma5Set = priceData.getDataSetByIndex(1);
        ILineDataSet ma10Set = priceData.getDataSetByIndex(2);
        ILineDataSet ma20Set = priceData.getDataSetByIndex(3);
        ILineDataSet ma30Set = priceData.getDataSetByIndex(4);
        CandleData kData = combinedData.getCandleData();
        ICandleDataSet klineSet = kData.getDataSetByIndex(0);
        IBarDataSet volSet = mChartVolume.getData().getBarData().getDataSetByIndex(0);
        IBarDataSet macdSet = mChartMacd.getData().getBarData().getDataSetByIndex(0);
        ILineDataSet difSet = mChartMacd.getData().getLineData().getDataSetByIndex(0);
        ILineDataSet deaSet = mChartMacd.getData().getLineData().getDataSetByIndex(1);
        LineData kdjData = mChartKdj.getData().getLineData();
        ILineDataSet kSet = kdjData.getDataSetByIndex(0);
        ILineDataSet dSet = kdjData.getDataSetByIndex(1);
        ILineDataSet jSet = kdjData.getDataSetByIndex(2);

        if (mData.contains(hisData)) {
            int index = mData.indexOf(hisData);
            klineSet.removeEntry(index);
            volSet.removeEntry(index);
            macdSet.removeEntry(index);
            difSet.removeEntry(index);
            deaSet.removeEntry(index);
            kSet.removeEntry(index);
            dSet.removeEntry(index);
            jSet.removeEntry(index);
            mData.remove(index);
        }
        mData.add(hisData);
//        mData = DataUtils.calculateHisData(mData);
//        hisData = getLastData();
        klineSet.addEntry(new CandleEntry(klineSet.getEntryCount(), (float) hisData.getHigh(), (float) hisData.getLow(), (float) hisData.getOpen(), (float) hisData.getClose()));
        volSet.addEntry(new BarEntry(volSet.getEntryCount(), hisData.getVol(), hisData));

        macdSet.addEntry(new BarEntry(macdSet.getEntryCount(), (float) hisData.getMacd()));
        difSet.addEntry(new Entry(difSet.getEntryCount(), (float) hisData.getDif()));
        deaSet.addEntry(new Entry(deaSet.getEntryCount(), (float) hisData.getDea()));

        kSet.addEntry(new Entry(kSet.getEntryCount(), (float) hisData.getK()));
        dSet.addEntry(new Entry(dSet.getEntryCount(), (float) hisData.getD()));
        jSet.addEntry(new Entry(jSet.getEntryCount(), (float) hisData.getJ()));

        ma5Set.addEntry(new Entry(ma5Set.getEntryCount(), (float) hisData.getMa5()));
        ma10Set.addEntry(new Entry(ma10Set.getEntryCount(), (float) hisData.getMa10()));
        ma20Set.addEntry(new Entry(ma20Set.getEntryCount(), (float) hisData.getMa20()));
        ma30Set.addEntry(new Entry(ma30Set.getEntryCount(), (float) hisData.getMa30()));


        mChartPrice.getXAxis().setAxisMaximum(combinedData.getXMax() + 1.5f);
        mChartVolume.getXAxis().setAxisMaximum(mChartVolume.getData().getXMax() + 1.5f);
        mChartMacd.getXAxis().setAxisMaximum(mChartMacd.getData().getXMax() + 1.5f);
        mChartKdj.getXAxis().setAxisMaximum(mChartKdj.getData().getXMax() + 1.5f);

        mChartPrice.notifyDataSetChanged();
        mChartPrice.invalidate();
        mChartVolume.notifyDataSetChanged();
        mChartVolume.invalidate();
        mChartMacd.notifyDataSetChanged();
        mChartMacd.invalidate();
        mChartKdj.notifyDataSetChanged();
        mChartKdj.invalidate();
    }

    public void addLineData(HisData hisData) {
        hisData = DataUtils.calculateHisData(hisData, mData);
        CombinedData combinedData = mChartPrice.getData();
        LineData priceData = combinedData.getLineData();
        ILineDataSet priceSet = priceData.getDataSetByIndex(0);
        ILineDataSet aveSet = priceData.getDataSetByIndex(1);
        IBarDataSet volSet = mChartVolume.getData().getBarData().getDataSetByIndex(0);
        if (mData.contains(hisData)) {
            int index = mData.indexOf(hisData);
            priceSet.removeEntry(index);
            aveSet.removeEntry(index);
            volSet.removeEntry(index);
            mData.remove(index);
        }
        mData.add(hisData);
        priceSet.addEntry(new Entry(priceSet.getEntryCount(), (float) hisData.getClose()));
        aveSet.addEntry(new Entry(aveSet.getEntryCount(), (float) hisData.getAvePrice()));
        volSet.addEntry(new BarEntry(volSet.getEntryCount(), hisData.getVol()));


        mChartPrice.getXAxis().setAxisMaximum(combinedData.getXMax() + 1.5f);
        mChartVolume.getXAxis().setAxisMaximum(mChartVolume.getData().getXMax() + 1.5f);

        mChartPrice.notifyDataSetChanged();
        mChartPrice.invalidate();
        mChartVolume.notifyDataSetChanged();
        mChartVolume.invalidate();
    }


    /**
     * align two chart
     */
    private void setOffset() {
        mChartPrice.setViewPortOffsets(0, 0, 5, DisplayUtils.dip2px(mContext, 20));
        mChartVolume.setViewPortOffsets(0, 0, 5, DisplayUtils.dip2px(mContext, 20));
        mChartMacd.setViewPortOffsets(0, 0, 5, DisplayUtils.dip2px(mContext, 20));
        mChartKdj.setViewPortOffsets(0, 0, 5, DisplayUtils.dip2px(mContext, 20));
        /*float lineLeft = mChartPrice.getViewPortHandler().offsetLeft();
        float barLeft = mChartVolume.getViewPortHandler().offsetLeft();
        float lineRight = mChartPrice.getViewPortHandler().offsetRight();
        float barRight = mChartVolume.getViewPortHandler().offsetRight();
        float offsetLeft, offsetRight;
        if (barLeft < lineLeft) {
            offsetLeft = Utils.convertPixelsToDp(lineLeft - barLeft);
            mChartVolume.setExtraLeftOffset(offsetLeft);
        } else {
            offsetLeft = Utils.convertPixelsToDp(barLeft - lineLeft);
            mChartPrice.setExtraLeftOffset(offsetLeft);
        }
        if (barRight < lineRight) {
            offsetRight = Utils.convertPixelsToDp(lineRight);
            mChartVolume.setExtraRightOffset(offsetRight);
        } else {
            offsetRight = Utils.convertPixelsToDp(barRight);
            mChartPrice.setExtraRightOffset(offsetRight);
        }*/

    }


    /**
     * set the count of line chart
     */
    public void setLineCount(int max, int min) {
        MAX_COUNT_LINE = max;
        MIN_COUNT_LINE = min;
    }

    /**
     * set the count of k chart
     */
    public void setKCount(int max, int min) {
        MAX_COUNT_K = max;
        MIN_COUNT_K = min;
    }

    /**
     * add limit line to chart
     */
    public void setLimitLine(double lastClose) {
        LimitLine limitLine = new LimitLine((float) lastClose);
        limitLine.enableDashedLine(5, 10, 0);
        limitLine.setLineColor(getResources().getColor(R.color.limit_color));
        mChartPrice.getAxisLeft().addLimitLine(limitLine);
    }

    public void setLimitLine() {
        setLimitLine(mLastClose);
    }

    public void setLastClose(double lastClose) {
        mLastClose = lastClose;
        mChartPrice.setOnChartValueSelectedListener(new InfoViewListener(mContext, mLastClose, mData, mChartInfoView, mChartVolume, mChartMacd, mChartKdj));
        mChartVolume.setOnChartValueSelectedListener(new InfoViewListener(mContext, mLastClose, mData, mChartInfoView, mChartPrice, mChartMacd, mChartKdj));
        mChartMacd.setOnChartValueSelectedListener(new InfoViewListener(mContext, mLastClose, mData, mChartInfoView, mChartPrice, mChartVolume, mChartKdj));
        mChartKdj.setOnChartValueSelectedListener(new InfoViewListener(mContext, mLastClose, mData, mChartInfoView, mChartPrice, mChartVolume, mChartMacd));
    }


    public void setDigits(int digits) {
        mDigits = digits;
    }

    public HisData getLastData() {
        if (mData != null && !mData.isEmpty()) {
            return mData.get(mData.size() - 1);
        }
        return null;
    }

}
