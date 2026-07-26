// 本项目密钥:w2013z0403
package com.mycompany.galbianji;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import android.graphics.*;

// ============================================================
// 数据模型
// ============================================================

class CondItem {
    public String condLine;
    public String storyText;
    public CondItem() { condLine = ""; storyText = ""; }
}

class OptionItem {
    public String optText;
    public boolean useShowCond;
    public String showCondLine;
    public int jumpId;
    public String varActionStr;
    public String showVarStr;
    public String hideVarStr;
    public OptionItem() {
        optText = "新选项";
        useShowCond = false;
        showCondLine = "";
        jumpId = -1;
        varActionStr = "";
        showVarStr = "";
        hideVarStr = "";
    }
}

class Module {
    public int id;
    public boolean useEnterCond;
    public String enterCondLine;
    public String defaultStory;
    public List<CondItem> condList;
    public boolean isEndNode;
    public String endStory;
    public List<OptionItem> optionList;
    public int enterJumpId;
    public int greenJumpId;
    public long idleTimeMs;
    public boolean haveSaveOpt;
    public String displayName;
    public String bgmName;
    public String bgImageName;

    public Module(int id) {
        this.id = id;
        useEnterCond = false;
        enterCondLine = "";
        defaultStory = "";
        condList = new ArrayList<CondItem>();
        isEndNode = false;
        endStory = "";
        optionList = new ArrayList<OptionItem>();
        enterJumpId = -1;
        greenJumpId = -1;
        idleTimeMs = 3000;
        haveSaveOpt = false;
        displayName = "";
        bgmName = "";
        bgImageName = "";
        EngineData.modMap.put(id, this);
    }
}

class GameNode {
    int id;
    float x, y, w, h;
    float inX, inY, outX, outY, topOutX, topOutY;
    float bottomOutX, bottomOutY;
    float btnW;
    GameNode(int id, float x, float y, float rate) {
        this.id = id;
        this.x = x;
        this.y = y;
        w = 520 * rate;
        h = 180 * rate;
        btnW = 90 * rate;
        refreshPos();
    }
    void refreshPos() {
        inX = x;
        inY = y + h / 2;
        outX = x + w;
        outY = y + h / 2;
        topOutX = x + w / 2f;
        topOutY = y;
        bottomOutX = x + w / 2f;
        bottomOutY = y + h;
    }
}

class LinePoint {
    public static final int TYPE_YELLOW = 0;
    public static final int TYPE_BLUE = 1;
    public static final int TYPE_GREEN = 2;
    int lineType;
    float cx, cy;
    int fromId, toId;
    LinePoint(int lineType, float cx, float cy, int fromId, int toId) {
        this.lineType = lineType;
        this.cx = cx;
        this.cy = cy;
        this.fromId = fromId;
        this.toId = toId;
    }
}

class GameVar {
    public String varName;
    public int baseValue;
    public boolean initShow;
    public String varType;
    public GameVar() {
        varName = "";
        baseValue = 0;
        initShow = false;
        varType = "normal";
    }
}

// ============================================================
// EngineData
// ============================================================

class EngineData {
    static Map<Integer, Module> modMap = new HashMap<Integer, Module>();
    static List<GameNode> nodeList = new ArrayList<GameNode>();
    static Map<String, Integer> gameVar = new HashMap<String, Integer>();
    static List<GameVar> varConfigList = new ArrayList<GameVar>();
    static List<String> showTopVar = new ArrayList<String>();
    static List<LinePoint> linePoints = new ArrayList<LinePoint>();
    static LinePoint selectedLinePoint = null;
    static int selectId = -1;
    static int dragBtn = -1;
    static Module selOptMod = null;
    static int selOptIdx = -1;
    static float scrollX = 0, scrollY = 0, scale = 1f;
    static boolean drawLine = false, drawTopLine = false, drawBottomLine = false;
    static GameNode lineStart = null, topLineStart = null, bottomLineStart = null;
    static float lineTouchX = 0, lineTouchY = 0;
    static boolean lineDirty = true;

    static int runNowId = 0;
    static String runStory = "";
    static List<OptionItem> runOptList = new ArrayList<OptionItem>();
    static boolean runIsEnd = false;

    static final int MAX_NORMAL_OPT = 19;
    static final int MAX_NOSAVE_OPT = 20;

    static String currentProjectPath = "";
    static String currentProjectName = "";
    static List<String> projectList = new ArrayList<String>();

    public static void clearAllData() {
        modMap.clear();
        nodeList.clear();
        linePoints.clear();
        selectedLinePoint = null;
        selectId = -1;
        selOptMod = null;
        selOptIdx = -1;
        scrollX = 0; scrollY = 0; scale = 1f;
        drawLine = false; drawTopLine = false; drawBottomLine = false;
        lineStart = null; topLineStart = null; bottomLineStart = null;
        lineDirty = true;
        gameVar.clear();
        showTopVar.clear();
        runOptList.clear();
        runStory = "";
        runIsEnd = false;
    }

    public static int getEmptyId() {
        int i = 0;
        while (true) {
            boolean f = false;
            for (int j = 0; j < nodeList.size(); j++) {
                if (nodeList.get(j).id == i) { f = true; break; }
            }
            if (!f) return i;
            i++;
        }
    }

    public static boolean hasSameLine(int fromId, int toId, int lineType) {
        if (lineType == LinePoint.TYPE_BLUE || lineType == LinePoint.TYPE_GREEN) return false;
        for (int i = 0; i < linePoints.size(); i++) {
            LinePoint lp = linePoints.get(i);
            if (lp.fromId == fromId && lp.toId == toId && lp.lineType == lineType) return true;
        }
        return false;
    }

    public static List<String> splitMultiCond(String line) {
        List<String> list = new ArrayList<String>();
        if (line == null || line.trim().length() == 0) return list;
        String[] arr = line.split(",");
        for (int i = 0; i < arr.length; i++) {
            String t = arr[i].trim();
            if (t.length() > 0) list.add(t);
        }
        return list;
    }

    public static boolean parseCond(String line, String[] res) {
        if (line == null || line.trim().length() == 0) return false;
        String[] arr = line.trim().split(" ");
        if (arr.length >= 3) {
            res[0] = arr[0]; res[1] = arr[1]; res[2] = arr[2];
            return true;
        }
        return false;
    }

    public static boolean isNumber(String s) {
        try { Integer.parseInt(s); return true; } catch (Exception e) { return false; }
    }

    public static int getVal(String key) {
        if (isNumber(key)) return Integer.parseInt(key);
        if (gameVar.containsKey(key)) return gameVar.get(key);
        return 0;
    }

    public static boolean checkSingleCond(String cond) {
        String[] p = new String[3];
        if (!parseCond(cond, p)) return true;
        int nowVal = gameVar.containsKey(p[0]) ? gameVar.get(p[0]) : 0;
        int tarVal = getVal(p[2]);
        if (p[1].equals(">=")) return nowVal >= tarVal;
        if (p[1].equals(">")) return nowVal > tarVal;
        if (p[1].equals("<=")) return nowVal <= tarVal;
        if (p[1].equals("<")) return nowVal < tarVal;
        if (p[1].equals("==")) return nowVal == tarVal;
        if (p[1].equals("!=")) return nowVal != tarVal;
        return false;
    }

    public static boolean checkMultiCond(String condLine) {
        List<String> list = splitMultiCond(condLine);
        for (int i = 0; i < list.size(); i++) {
            if (!checkSingleCond(list.get(i))) return false;
        }
        return true;
    }

    public static String formatStory(String text) {
        String result = text;
        Set<String> keys = gameVar.keySet();
        String[] arr = keys.toArray(new String[0]);
        for (int i = 0; i < arr.length; i++) {
            String tag = "<" + arr[i] + ">";
            if (result.contains(tag)) {
                result = result.replace(tag, String.valueOf(gameVar.get(arr[i])));
            }
        }
        return result;
    }

    public static void execVarAction(String actionStr) {
        if (actionStr == null || actionStr.trim().length() == 0) return;
        String[] parts = actionStr.split(",");
        if (parts.length < 3) return;
        String vName = parts[0].trim();
        String op = parts[1].trim();
        int now = gameVar.containsKey(vName) ? gameVar.get(vName) : 0;
        int tVal = getVal(parts[2].trim());
        if (op.equals("set")) gameVar.put(vName, tVal);
        else if (op.equals("add")) gameVar.put(vName, now + tVal);
        else if (op.equals("sub")) gameVar.put(vName, now - tVal);
        else if (op.equals("mul")) gameVar.put(vName, now * tVal);
        else if (op.equals("div") && tVal != 0) gameVar.put(vName, now / tVal);
    }

    public static void execMultiVarAction(String allStr) {
        if (allStr == null || allStr.trim().length() == 0) return;
        String[] group = allStr.split(";");
        for (int i = 0; i < group.length; i++) {
            execVarAction(group[i]);
        }
    }

    public static void parseShowHideVar(String str, List<String> targetList) {
        targetList.clear();
        if (str == null || str.trim().length() == 0) return;
        String[] arr = str.split(",");
        for (int i = 0; i < arr.length; i++) {
            String t = arr[i].trim();
            if (t.length() > 0) targetList.add(t);
        }
    }

    public static void refreshRunData() {
        runOptList.clear();
        if (!modMap.containsKey(runNowId)) {
            runStory = "剧情结束";
            runIsEnd = true;
            return;
        }
        Module cur = modMap.get(runNowId);

        for (int i = 0; i < varConfigList.size(); i++) {
            GameVar gv = varConfigList.get(i);
            if ("random".equals(gv.varType)) {
                gameVar.put(gv.varName, (int)(Math.random() * 10001));
            }
        }

        if (cur.useEnterCond) {
            if (!checkMultiCond(cur.enterCondLine) && cur.enterJumpId != -1) {
                runNowId = cur.enterJumpId;
                refreshRunData();
                return;
            }
        }

        runStory = formatStory(cur.defaultStory);
        for (int i = 0; i < cur.condList.size(); i++) {
            CondItem ci = cur.condList.get(i);
            if (checkMultiCond(ci.condLine)) {
                runStory = formatStory(ci.storyText);
                break;
            }
        }

        if (cur.isEndNode) {
            runStory = runStory + "\n" + formatStory(cur.endStory);
            runIsEnd = true;
            return;
        }
        runIsEnd = false;

        if (cur.haveSaveOpt) {
            OptionItem saveOpt = new OptionItem();
            saveOpt.optText = "保存游戏";
            saveOpt.jumpId = runNowId;
            runOptList.add(saveOpt);
        }

        int maxOpt = cur.haveSaveOpt ? 19 : 20;
        for (int i = 0; i < cur.optionList.size() && runOptList.size() < maxOpt; i++) {
            OptionItem o = cur.optionList.get(i);
            boolean canShow = true;
            if (o.useShowCond) canShow = checkMultiCond(o.showCondLine);
            if (canShow) runOptList.add(o);
        }
    }

    public static GameNode getNode(int id) {
        for (int i = 0; i < nodeList.size(); i++) {
            if (nodeList.get(i).id == id) return nodeList.get(i);
        }
        return null;
    }
}

// ============================================================
// ScaleListen
// ============================================================

class ScaleListen extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    private DrawCanvas canvas;
    private float focusOffsetX = 0, focusOffsetY = 0;
    public ScaleListen(DrawCanvas canvas) { this.canvas = canvas; }

    public boolean onScaleBegin(ScaleGestureDetector d) {
        canvas.isScaling = true;
        float fx = d.getFocusX(), fy = d.getFocusY();
        focusOffsetX = (fx - EngineData.scrollX) / EngineData.scale;
        focusOffsetY = (fy - EngineData.scrollY) / EngineData.scale;
        return true;
    }

    public boolean onScale(ScaleGestureDetector d) {
        float factor = d.getScaleFactor();
        float newScale = EngineData.scale * (1f + (factor - 1f) * 0.25f);
        newScale = Math.max(0.1f, Math.min(newScale, 5.0f));
        float fx = d.getFocusX(), fy = d.getFocusY();
        EngineData.scrollX = fx - focusOffsetX * newScale;
        EngineData.scrollY = fy - focusOffsetY * newScale;
        float maxScroll = 2000f * newScale;
        EngineData.scrollX = Math.max(-maxScroll, Math.min(EngineData.scrollX, maxScroll));
        EngineData.scrollY = Math.max(-maxScroll, Math.min(EngineData.scrollY, maxScroll));
        EngineData.scale = newScale;
        canvas.safeInvalidate();
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector d) { canvas.isScaling = false; }
}

// ============================================================
// DrawCanvas（完整重构版）
// ============================================================

class DrawCanvas extends View {
    private Paint gridPaint = new Paint();
    private Paint linePaint = new Paint();
    private Paint nodePaint = new Paint();
    private Paint textPaint = new Paint();
    private Paint arrowPaint = new Paint();
    private Path cachePath = new Path();
    private RectF cacheRect = new RectF();

    float lastX = 0, lastY = 0;
    boolean dragNode = false;
    int sW = 0, sH = 0;
    float rate = 1f;
    ScaleGestureDetector scaleDet;
    final int GRID = 60;
    public int pageScene = 0;
    private final float pointR = 40f;
    private final float touchR = 40f;
    boolean isScaling = false;

    private float downX = 0, downY = 0;
    private static final float TOUCH_SLOP = 8f;
    private long lastDrawTime = 0;
    private static final long DRAW_INTERVAL = 16;
    private long lastClickTime = 0;
    private static final long CLICK_DEBOUNCE = 300;

    private float optScrollY = 0f;
    private float touchDownY = 0f;
    private boolean isTouchOptArea = false;

    private MediaPlayer bgmPlayer = null;
    private MainActivity mainActivity;

    private boolean isSidebarOpen = false;
    private static final float SIDEBAR_W = 280;
    private static final int TAB_IMAGE = 0;
    private static final int TAB_AUDIO = 1;
    private static final int TAB_FUNC = 2;
    private int currentTab = TAB_IMAGE;
    private Handler longPressHandler = new Handler();
    private Runnable longPressRunnable = null;
    private static final long LONG_PRESS_DELAY = 800;

    private String draggingImageName = null;
    private float dragImageX = 0, dragImageY = 0;
    private boolean isDraggingImage = false;
    private long imageDownTime = 0;
    private static final long DRAG_DELAY = 1800;

    private boolean isSliding = false;

    private Timer idleTimer = null;
    private boolean isIdleWaiting = false;

    public DrawCanvas(Context c) {
        super(c);
        mainActivity = (MainActivity) c;
        scaleDet = new ScaleGestureDetector(getContext(), new ScaleListen(this));
        setWillNotDraw(false);
        gridPaint.setAntiAlias(false);
        gridPaint.setColor(0xff232323);
        gridPaint.setStrokeWidth(1);
        linePaint.setAntiAlias(true);
        nodePaint.setAntiAlias(true);
        textPaint.setAntiAlias(true);
        arrowPaint.setAntiAlias(true);
        setClickable(true);
        setFocusable(true);
    }

    public void safeInvalidate() {
        long now = System.currentTimeMillis();
        if (now - lastDrawTime >= DRAW_INTERVAL) {
            invalidate();
            postInvalidate();
            lastDrawTime = now;
        }
    }

    protected void onSizeChanged(int w, int h, int ow, int oh) {
        sW = w; sH = h; rate = sW / 1080f;
        safeInvalidate();
    }

    float snap(float v) {
        float g = GRID;
        float r = Math.round(v / g) * g;
        return Math.abs(v - r) < g * 0.3f ? r : v;
    }

    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (sW == 0 || sH == 0) {
            c.drawColor(0xFF1a1a2e);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(60);
            c.drawText("初始化...", 100, 200, textPaint);
            return;
        }
        c.drawColor(0xFF1a1a2e);

        if (pageScene == 1) { drawGameRunPage(c); return; }
        if (pageScene == 2) { drawProjectOverview(c); return; }

        if (EngineData.nodeList == null || EngineData.nodeList.size() == 0) {
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(50);
            c.drawText("点击左侧菜单「创建节点」", 100, 200, textPaint);
            textPaint.setTextSize(30);
            c.drawText("或从相册分享图片到本应用", 100, 280, textPaint);
            drawSidebar(c);
            drawTopButtons(c);
            drawZoomSlider(c);
            return;
        }
        drawEditorPage(c);
    }

    void drawEditorPage(Canvas c) {
        c.clipRect(0, 0, sW, sH);
        nodePaint.setColor(0xff161616);
        c.drawRect(0, 0, sW, sH, nodePaint);
        c.save();
        c.scale(EngineData.scale, EngineData.scale);
        c.translate(EngineData.scrollX / EngineData.scale, EngineData.scrollY / EngineData.scale);

        float viewLeft = -EngineData.scrollX / EngineData.scale;
        float viewTop = -EngineData.scrollY / EngineData.scale;
        float viewRight = viewLeft + sW / EngineData.scale;
        float viewBot = viewTop + sH / EngineData.scale;

        for (float x = (float)Math.floor(viewLeft/GRID)*GRID - GRID; x <= viewRight + GRID; x += GRID)
            c.drawLine(x, viewTop - GRID, x, viewBot + GRID, gridPaint);
        for (float y = (float)Math.floor(viewTop/GRID)*GRID - GRID; y <= viewBot + GRID; y += GRID)
            c.drawLine(viewLeft - GRID, y, viewRight + GRID, y, gridPaint);

        rebuildLines();
        drawAllLines(c);
        drawTempLines(c);
        drawLinePoints(c);

        for (int i = 0; i < EngineData.nodeList.size(); i++) {
            GameNode nd = EngineData.nodeList.get(i);
            nd.refreshPos();
            if (nd.x > viewRight || nd.x + nd.w < viewLeft || nd.y > viewBot || nd.y + nd.h < viewTop) continue;
            drawNode(c, nd);
        }
        c.restore();

        drawSidebar(c);
        drawTopButtons(c);
        drawZoomSlider(c);

        if (isDraggingImage && draggingImageName != null) {
            float imgSize = 80 * rate;
            nodePaint.setColor(0xCC000000);
            c.drawRect(dragImageX - imgSize/2 - 10, dragImageY - imgSize/2 - 10,
					   dragImageX + imgSize/2 + 10, dragImageY + imgSize/2 + 10, nodePaint);
            textPaint.setColor(0xFFFFFF44);
            textPaint.setTextSize(18 * rate);
            textPaint.setTextAlign(Paint.Align.CENTER);
            c.drawText("📷 " + draggingImageName, dragImageX, dragImageY + 5 * rate, textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    void drawZoomSlider(Canvas c) {
        float sliderW = 20 * rate;
        float sliderH = sH * 0.5f;
        float sliderX = sW - sliderW - 20 * rate;
        float sliderY = (sH - sliderH) / 2;

        nodePaint.setColor(0x66444466);
        c.drawRect(sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, nodePaint);

        float thumbY = sliderY + (1 - (EngineData.scale - 0.1f) / 4.9f) * (sliderH - 40 * rate);
        nodePaint.setColor(0xFF88CCFF);
        c.drawRect(sliderX - 10 * rate, thumbY - 15 * rate,
				   sliderX + sliderW + 10 * rate, thumbY + 15 * rate, nodePaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(14 * rate);
        textPaint.setTextAlign(Paint.Align.CENTER);
        c.drawText("+", sliderX + sliderW / 2, sliderY + 25 * rate, textPaint);
        c.drawText("-", sliderX + sliderW / 2, sliderY + sliderH - 10 * rate, textPaint);
        c.drawText((int)(EngineData.scale * 100) + "%", sliderX + sliderW / 2,
				   sliderY + sliderH / 2 + 5 * rate, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    void rebuildLines() {
        if (!EngineData.lineDirty) return;
        EngineData.linePoints.clear();

        for (int i = 0; i < EngineData.nodeList.size(); i++) {
            GameNode nd = EngineData.nodeList.get(i);
            nd.refreshPos();
            Module m = EngineData.modMap.get(nd.id);
            if (m == null) continue;

            if (m.enterJumpId != -1) {
                GameNode tar = EngineData.getNode(m.enterJumpId);
                if (tar != null && tar.id != nd.id) {
                    tar.refreshPos();
                    if (!EngineData.hasSameLine(nd.id, tar.id, LinePoint.TYPE_BLUE)) {
                        float midX = (nd.topOutX + tar.inX) / 2f;
                        float midY = (nd.topOutY + tar.inY) / 2f;
                        EngineData.linePoints.add(new LinePoint(LinePoint.TYPE_BLUE, midX, midY, nd.id, tar.id));
                    }
                }
            }

            if (m.greenJumpId != -1) {
                GameNode tar = EngineData.getNode(m.greenJumpId);
                if (tar != null && tar.id != nd.id) {
                    tar.refreshPos();
                    if (!EngineData.hasSameLine(nd.id, tar.id, LinePoint.TYPE_GREEN)) {
                        float midX = (nd.bottomOutX + tar.inX) / 2f;
                        float midY = (nd.bottomOutY + tar.inY) / 2f;
                        EngineData.linePoints.add(new LinePoint(LinePoint.TYPE_GREEN, midX, midY, nd.id, tar.id));
                    }
                }
            }

            for (int j = 0; j < m.optionList.size(); j++) {
                OptionItem opt = m.optionList.get(j);
                GameNode tar = EngineData.getNode(opt.jumpId);
                if (tar != null && tar.id != nd.id) {
                    tar.refreshPos();
                    if (!EngineData.hasSameLine(nd.id, tar.id, LinePoint.TYPE_YELLOW)) {
                        float midX = (nd.outX + tar.inX) / 2f;
                        float midY = (nd.outY + tar.inY) / 2f;
                        EngineData.linePoints.add(new LinePoint(LinePoint.TYPE_YELLOW, midX, midY, nd.id, tar.id));
                    }
                }
            }
        }
        EngineData.lineDirty = false;
    }

    void drawAllLines(Canvas c) {
        for (int i = 0; i < EngineData.linePoints.size(); i++) {
            LinePoint lp = EngineData.linePoints.get(i);
            GameNode from = EngineData.getNode(lp.fromId);
            GameNode to = EngineData.getNode(lp.toId);
            if (from == null || to == null) continue;
            from.refreshPos(); to.refreshPos();

            int color = 0xffffd250;
            float width = 4;
            if (lp.lineType == LinePoint.TYPE_BLUE) { color = 0xff50b4ff; width = 3; }
            else if (lp.lineType == LinePoint.TYPE_GREEN) { color = 0xff44dd44; width = 3; }

            boolean selected = (EngineData.selectedLinePoint == lp);
            linePaint.setColor(selected ? 0xffffff80 : color);
            linePaint.setStrokeWidth(selected ? width + 3 : width);
            linePaint.setStyle(Paint.Style.STROKE);

            float sx, sy, ex, ey;
            if (lp.lineType == LinePoint.TYPE_BLUE) { sx = from.topOutX; sy = from.topOutY; ex = to.inX; ey = to.inY; }
            else if (lp.lineType == LinePoint.TYPE_GREEN) { sx = from.bottomOutX; sy = from.bottomOutY; ex = to.inX; ey = to.inY; }
            else { sx = from.outX; sy = from.outY; ex = to.inX; ey = to.inY; }

            cachePath.reset();
            cachePath.moveTo(sx, sy);
            cachePath.cubicTo((sx + ex) / 2, sy, (sx + ex) / 2, ey, ex, ey);
            c.drawPath(cachePath, linePaint);
            drawArrow(c, ex, ey, sx, sy, color);
        }
    }

    void drawTempLines(Canvas c) {
        if (EngineData.drawLine && EngineData.lineStart != null) {
            linePaint.setColor(0xffffd250); linePaint.setStrokeWidth(4); linePaint.setStyle(Paint.Style.STROKE);
            cachePath.reset();
            cachePath.moveTo(EngineData.lineStart.outX, EngineData.lineStart.outY);
            cachePath.cubicTo((EngineData.lineStart.outX + EngineData.lineTouchX)/2, EngineData.lineStart.outY,
							  (EngineData.lineStart.outX + EngineData.lineTouchX)/2, EngineData.lineTouchY,
							  EngineData.lineTouchX, EngineData.lineTouchY);
            c.drawPath(cachePath, linePaint);
        }
        if (EngineData.drawTopLine && EngineData.topLineStart != null) {
            linePaint.setColor(0xff50b4ff); linePaint.setStrokeWidth(3); linePaint.setStyle(Paint.Style.STROKE);
            cachePath.reset();
            cachePath.moveTo(EngineData.topLineStart.topOutX, EngineData.topLineStart.topOutY);
            cachePath.cubicTo((EngineData.topLineStart.topOutX + EngineData.lineTouchX)/2, EngineData.topLineStart.topOutY,
							  (EngineData.topLineStart.topOutX + EngineData.lineTouchX)/2, EngineData.lineTouchY,
							  EngineData.lineTouchX, EngineData.lineTouchY);
            c.drawPath(cachePath, linePaint);
        }
        if (EngineData.drawBottomLine && EngineData.bottomLineStart != null) {
            linePaint.setColor(0xff44dd44); linePaint.setStrokeWidth(3); linePaint.setStyle(Paint.Style.STROKE);
            cachePath.reset();
            cachePath.moveTo(EngineData.bottomLineStart.bottomOutX, EngineData.bottomLineStart.bottomOutY);
            cachePath.cubicTo((EngineData.bottomLineStart.bottomOutX + EngineData.lineTouchX)/2, EngineData.bottomLineStart.bottomOutY,
							  (EngineData.bottomLineStart.bottomOutX + EngineData.lineTouchX)/2, EngineData.lineTouchY,
							  EngineData.lineTouchX, EngineData.lineTouchY);
            c.drawPath(cachePath, linePaint);
        }
    }

    void drawLinePoints(Canvas c) {
        linePaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < EngineData.linePoints.size(); i++) {
            LinePoint lp = EngineData.linePoints.get(i);
            c.drawCircle(lp.cx, lp.cy, pointR, linePaint);
        }
        linePaint.setStyle(Paint.Style.STROKE);
    }

    void drawNode(Canvas c, GameNode nd) {
        Module m = EngineData.modMap.get(nd.id);
        String name = (m != null && m.displayName != null && m.displayName.length() > 0) ? m.displayName : "节点"+nd.id;

        nodePaint.setColor(nd.id == EngineData.selectId ? 0xff484848 : 0xff2d2d2d);
        cacheRect.set(nd.x, nd.y, nd.x + nd.w, nd.y + nd.h);
        c.drawRect(cacheRect, nodePaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Math.min(44, nd.w / Math.max(name.length(), 1) * 2) * rate);
        c.drawText(name, nd.x + 30*rate, nd.y + nd.h/2 + 12*rate, textPaint);

        nodePaint.setColor(0xff3c3c3c);
        float b1x = nd.x + nd.w - 2 * nd.btnW;
        cacheRect.set(b1x, nd.y + 15*rate, b1x + nd.btnW, nd.y + nd.h - 15*rate);
        c.drawRect(cacheRect, nodePaint);
        float b2x = nd.x + nd.w - nd.btnW;
        cacheRect.set(b2x, nd.y + 15*rate, b2x + nd.btnW, nd.y + nd.h - 15*rate);
        c.drawRect(cacheRect, nodePaint);
        textPaint.setColor(0xffeeeeee);
        textPaint.setTextSize(30*rate);
        c.drawText("编", b1x + nd.btnW/2 - 10*rate, nd.y + nd.h/2 + 10*rate, textPaint);
        c.drawText("删", b2x + nd.btnW/2 - 10*rate, nd.y + nd.h/2 + 10*rate, textPaint);

        arrowPaint.setColor(0xff999999);
        float s = 12*rate;
        cachePath.reset();
        cachePath.moveTo(nd.inX, nd.inY);
        cachePath.lineTo(nd.inX + s, nd.inY - s);
        cachePath.lineTo(nd.inX + s, nd.inY + s);
        cachePath.close();
        c.drawPath(cachePath, arrowPaint);

        linePaint.setColor(0xff50b4ff);
        float cr = 10*rate;
        c.drawLine(nd.topOutX - cr, nd.topOutY, nd.topOutX + cr, nd.topOutY, linePaint);
        c.drawLine(nd.topOutX, nd.topOutY - cr, nd.topOutX, nd.topOutY + cr, linePaint);
        linePaint.setColor(0xff44dd44);
        c.drawLine(nd.bottomOutX - cr, nd.bottomOutY, nd.bottomOutX + cr, nd.bottomOutY, linePaint);
        c.drawLine(nd.bottomOutX, nd.bottomOutY - cr, nd.bottomOutX, nd.bottomOutY + cr, linePaint);
    }

    void drawArrow(Canvas c, float x, float y, float fx, float fy, int color) {
        float a = (float)Math.atan2(y - fy, x - fx);
        float len = 22;
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setColor(color);
        float p1x = x - (float)Math.cos(a - 0.4f) * len;
        float p1y = y - (float)Math.sin(a - 0.4f) * len;
        float p2x = x - (float)Math.cos(a + 0.4f) * len;
        float p2y = y - (float)Math.sin(a + 0.4f) * len;
        cachePath.reset();
        cachePath.moveTo(x, y);
        cachePath.lineTo(p1x, p1y);
        cachePath.lineTo(p2x, p2y);
        cachePath.close();
        c.drawPath(cachePath, arrowPaint);
    }

    void drawSidebar(Canvas c) {
        float sidebarW = SIDEBAR_W * rate;
        float tabW = 25 * rate;
        float tabH = sH / 3f;

        for (int i = 0; i < 3; i++) {
            float y = i * tabH;
            int color = (currentTab == i) ? 0xCC88CCFF : 0x88444488;
            nodePaint.setColor(color);
            c.drawRect(0, y, tabW, y + tabH, nodePaint);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(14 * rate);
            textPaint.setTextAlign(Paint.Align.CENTER);
            String label = (i == 0) ? "图" : (i == 1) ? "音" : "功";
            c.drawText(label, tabW / 2, y + tabH / 2 + 5 * rate, textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
        }

        if (!isSidebarOpen) return;

        nodePaint.setColor(0xDD222244);
        c.drawRect(0, 0, sidebarW, sH, nodePaint);
        nodePaint.setColor(0xFF333366);
        c.drawRect(sidebarW - 2, 0, sidebarW, sH, nodePaint);

        float contentX = tabW + 15 * rate;
        float contentY = 30 * rate;
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22 * rate);

        if (currentTab == TAB_IMAGE) {
            c.drawText("📷 图片", contentX, contentY, textPaint);
            contentY += 30 * rate;
            List<File> images = mainActivity.getImportedImages();
            for (int i = 0; i < images.size(); i++) {
                File f = images.get(i);
                String name = f.getName();
                if (name.length() > 18) name = name.substring(0, 15) + "..";
                textPaint.setTextSize(17 * rate);
                if (draggingImageName != null && draggingImageName.equals(f.getName())) {
                    textPaint.setColor(0xFFFFCC44);
                } else {
                    textPaint.setColor(Color.WHITE);
                }
                c.drawText((i + 1) + "." + name, contentX, contentY, textPaint);
                contentY += 26 * rate;
                if (contentY > sH - 40) break;
            }
            if (images.size() == 0) {
                textPaint.setTextSize(16 * rate);
                textPaint.setColor(0xFF888888);
                c.drawText("暂无图片，请从相册分享", contentX, contentY, textPaint);
            }
        } else if (currentTab == TAB_AUDIO) {
            c.drawText("🎵 音频", contentX, contentY, textPaint);
            contentY += 30 * rate;
            List<File> audios = mainActivity.getImportedAudios();
            for (int i = 0; i < audios.size(); i++) {
                File f = audios.get(i);
                String name = f.getName();
                if (name.length() > 18) name = name.substring(0, 15) + "..";
                textPaint.setTextSize(17 * rate);
                textPaint.setColor(Color.WHITE);
                c.drawText((i + 1) + "." + name, contentX, contentY, textPaint);
                contentY += 26 * rate;
                if (contentY > sH - 40) break;
            }
            if (audios.size() == 0) {
                textPaint.setTextSize(16 * rate);
                textPaint.setColor(0xFF888888);
                c.drawText("暂无音频，请从相册分享", contentX, contentY, textPaint);
            }
        } else {
            c.drawText("⚡ 功能", contentX, contentY, textPaint);
            contentY += 35 * rate;
            String[] funcs = {"创建节点", "创建变量", "返回主页", "查看介绍", "项目总览", "变量管理"};
            for (int i = 0; i < funcs.length; i++) {
                textPaint.setTextSize(18 * rate);
                textPaint.setColor(Color.WHITE);
                c.drawText("● " + funcs[i], contentX, contentY, textPaint);
                contentY += 34 * rate;
            }
        }
    }

    void drawTopButtons(Canvas c) {
        String[] btnTexts = {"项目", "运行", "保存", "存档", "导入", "设置"};
        float btnW = 72 * rate, btnH = 40 * rate;
        float x = sW - (btnTexts.length * (btnW + 8 * rate)) - 16 * rate;
        float y = 10 * rate;
        for (int i = 0; i < btnTexts.length; i++) {
            nodePaint.setColor(0xFF444466);
            cacheRect.set(x + i * (btnW + 8 * rate), y, x + i * (btnW + 8 * rate) + btnW, y + btnH);
            c.drawRect(cacheRect, nodePaint);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(20 * rate);
            float tw = textPaint.measureText(btnTexts[i]);
            c.drawText(btnTexts[i], x + i * (btnW + 8 * rate) + (btnW - tw)/2, y + btnH/2 + 7*rate, textPaint);
        }
    }

    void drawGameRunPage(Canvas c) {
        c.drawColor(0xFF1a1a2e);
		// ===== 画当前节点的背景图 =====
		if (EngineData.modMap.containsKey(EngineData.runNowId)) {
			Module cur = EngineData.modMap.get(EngineData.runNowId);
			if (cur.bgImageName != null && cur.bgImageName.length() > 0) {
				File imgFile = mainActivity.getImageFile(cur.bgImageName);
				if (imgFile != null && imgFile.exists()) {
					try {
						Bitmap bmp = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
						if (bmp != null) {
							// 全屏铺满
							float scaleX = (float)sW / bmp.getWidth();
							float scaleY = (float)sH / bmp.getHeight();
							float scale = Math.max(scaleX, scaleY);
							int dw = (int)(bmp.getWidth() * scale);
							int dh = (int)(bmp.getHeight() * scale);
							int dx = (sW - dw) / 2;
							int dy = (sH - dh) / 2;
							Rect srcRect = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
							Rect dstRect = new Rect(dx, dy, dx + dw, dy + dh);
							c.drawBitmap(bmp, srcRect, dstRect, null);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

        nodePaint.setColor(0xCCFF4444);
        cacheRect.set(20*rate, 20*rate, 120*rate, 70*rate);
        c.drawRect(cacheRect, nodePaint);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30*rate);
        c.drawText("← 退出", 30*rate, 58*rate, textPaint);

        if (EngineData.modMap.containsKey(EngineData.runNowId)) {
            Module cur = EngineData.modMap.get(EngineData.runNowId);
            if (cur.displayName != null && cur.displayName.length() > 0) {
                textPaint.setColor(0xFF88CCFF);
                textPaint.setTextSize(28*rate);
                c.drawText("[" + cur.displayName + "]", 140*rate, 50*rate, textPaint);
            }
        }

        float contentH = sH - 180*rate;
        float optScrollY = this.optScrollY;

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32*rate);
        float varY = 70*rate;
        for (int i = 0; i < EngineData.showTopVar.size(); i++) {
            String vName = EngineData.showTopVar.get(i);
            int val = EngineData.gameVar.containsKey(vName) ? EngineData.gameVar.get(vName) : 0;
            c.drawText(vName + ":" + val, 30*rate, varY, textPaint);
            varY += 35*rate;
        }

        textPaint.setTextSize(30*rate);
        float optY = 70*rate + optScrollY;
        for (int i = 0; i < EngineData.runOptList.size(); i++) {
            OptionItem opt = EngineData.runOptList.get(i);
            nodePaint.setColor(0xFF323256);
            cacheRect.set(sW/2 + 20*rate, optY, sW - 20*rate, optY + 56*rate);
            c.drawRect(cacheRect, nodePaint);
            textPaint.setColor(Color.WHITE);
            String displayText = opt.optText;
            if (displayText.length() > 15) displayText = displayText.substring(0, 14) + "...";
            c.drawText(displayText, sW/2 + 40*rate, optY + 36*rate, textPaint);
            optY += 66*rate;
        }

        textPaint.setTextSize(34*rate);
        textPaint.setColor(Color.WHITE);
        float textX = 30*rate;
        float textY = contentH + 40*rate;
        float textMaxWidth = sW - 60*rate;
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float lineHeight = fm.descent - fm.ascent + fm.leading;

        String fullStory = EngineData.runStory;
        int start = 0;
        while (start < fullStory.length()) {
            int end = textPaint.breakText(fullStory, start, fullStory.length(), true, textMaxWidth, null);
            if (end <= 0) break;
            c.drawText(fullStory.substring(start, start + end), textX, textY, textPaint);
            textY += lineHeight;
            start += end;
        }

        if (EngineData.runIsEnd) {
            textPaint.setTextSize(40*rate);
            textPaint.setColor(0xFFFF6666);
            c.drawText("已到达结局", 60*rate, sH - 60*rate, textPaint);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(28*rate);
            c.drawText("点击退出按钮返回", 60*rate, sH - 30*rate, textPaint);
        }
    }

    void drawProjectOverview(Canvas c) {
		c.drawColor(0xFF1a1a2e);
		textPaint.setColor(Color.WHITE);
		textPaint.setTextSize(45*rate);
		c.drawText("📁 项目总览", sW/2 - 120*rate, 60*rate, textPaint);

		textPaint.setTextSize(28*rate);
		float y = 100*rate;

		c.drawText("当前: " + EngineData.currentProjectName, 40*rate, y, textPaint);
		y += 40*rate;

		c.drawText("节点: " + EngineData.nodeList.size() + "  |  变量: " + EngineData.varConfigList.size() + "  |  存档: " + mainActivity.getSaveCount(), 40*rate, y, textPaint);
		y += 50*rate;

		String[] actions = {"切换项目", "新建项目", "重命名项目", "删除项目", "复制项目"};
		for (int i = 0; i < actions.length; i++) {
			float bx = 40*rate + i * (130*rate + 10*rate);
			nodePaint.setColor(0xFF444466);
			cacheRect.set(bx, y, bx + 130*rate, y + 38*rate);
			c.drawRect(cacheRect, nodePaint);
			textPaint.setColor(Color.WHITE);
			textPaint.setTextSize(18*rate);
			float tw = textPaint.measureText(actions[i]);
			c.drawText(actions[i], bx + (130*rate - tw)/2, y + 26*rate, textPaint);
		}
		y += 50*rate;

		nodePaint.setColor(0xFF444466);
		c.drawRect(40*rate, y, sW - 40*rate, y + 2*rate, nodePaint);
		y += 30*rate;

		textPaint.setColor(0xFF88CCFF);
		textPaint.setTextSize(30*rate);
		c.drawText("💾 存档管理", 40*rate, y, textPaint);
		y += 40*rate;

		String[] saveActions = {"切换存档", "新建存档", "重命名存档", "删除存档", "复制存档"};
		for (int i = 0; i < saveActions.length; i++) {
			float bx = 40*rate + i * (130*rate + 10*rate);
			nodePaint.setColor(0xFF446644);
			cacheRect.set(bx, y, bx + 130*rate, y + 38*rate);
			c.drawRect(cacheRect, nodePaint);
			textPaint.setColor(Color.WHITE);
			textPaint.setTextSize(18*rate);
			float tw = textPaint.measureText(saveActions[i]);
			c.drawText(saveActions[i], bx + (130*rate - tw)/2, y + 26*rate, textPaint);
		}
		y += 60*rate;

		// 存档列表
		textPaint.setTextSize(22*rate);
		textPaint.setColor(0xFFAAAAAA);
		File saveDir = new File(mainActivity.getCurrentProjectPath() + "/saves");
		if (saveDir.exists()) {
			File[] saves = saveDir.listFiles(new FilenameFilter() {
					public boolean accept(File d, String name) { return name.endsWith(".sav"); }
				});
			if (saves != null) {
				for (int i = 0; i < saves.length && i < 5; i++) {
					c.drawText("  " + (i+1) + ". " + saves[i].getName().replace(".sav", ""), 40*rate, y, textPaint);
					y += 30*rate;
				}
			}
		}

		// ===== 返回按钮（右下角） =====
		float backX = sW - 160 * rate;
		float backY = sH - 70 * rate;
		nodePaint.setColor(0xFF4466AA);
		cacheRect.set(backX, backY, backX + 120*rate, backY + 50*rate);
		c.drawRect(cacheRect, nodePaint);
		textPaint.setColor(Color.WHITE);
		textPaint.setTextSize(26*rate);
		textPaint.setTextAlign(Paint.Align.CENTER);
		c.drawText("← 返回", backX + 60*rate, backY + 34*rate, textPaint);
		textPaint.setTextAlign(Paint.Align.LEFT);
	}

    // ============================================================
    // 触摸事件（完整保留你的原有逻辑）
    // ============================================================

    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        float sidebarW = SIDEBAR_W * rate;

        long now = System.currentTimeMillis();
        if (e.getAction() == MotionEvent.ACTION_UP) {
            if (now - lastClickTime < CLICK_DEBOUNCE) {
                return true;
            }
            lastClickTime = now;
        }

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            float sliderW = 20 * rate;
            float sliderH = sH * 0.5f;
            float sliderX = sW - sliderW - 20 * rate;
            float sliderY = (sH - sliderH) / 2;
            if (x >= sliderX - 30 * rate && x <= sliderX + sliderW + 30 * rate
				&& y >= sliderY && y <= sliderY + sliderH) {
                isSliding = true;
                float progress = (y - sliderY) / sliderH;
                float newScale = 0.1f + (1 - progress) * 4.9f;
                EngineData.scale = Math.max(0.1f, Math.min(newScale, 5.0f));
                safeInvalidate();
                return true;
            }
        }
        if (e.getAction() == MotionEvent.ACTION_MOVE && isSliding) {
            float sliderW = 20 * rate;
            float sliderH = sH * 0.5f;
            float sliderX = sW - sliderW - 20 * rate;
            float sliderY = (sH - sliderH) / 2;
            float progress = (y - sliderY) / sliderH;
            float newScale = 0.1f + (1 - progress) * 4.9f;
            EngineData.scale = Math.max(0.1f, Math.min(newScale, 5.0f));
            safeInvalidate();
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
            isSliding = false;
        }

        if (pageScene == 1) return handleRunTouch(e);
        if (pageScene == 2) return handleOverviewTouch(e);

        if (isSidebarOpen && x < sidebarW) {
            return handleSidebarTouch(e);
        }

        if (e.getAction() == MotionEvent.ACTION_DOWN && x < 30 * rate) {
            isSidebarOpen = true;
            safeInvalidate();
            return true;
        }

        if (isSidebarOpen && e.getAction() == MotionEvent.ACTION_DOWN) {
            isSidebarOpen = false;
            safeInvalidate();
            return true;
        }

        if (e.getAction() == MotionEvent.ACTION_DOWN && isSidebarOpen && currentTab == TAB_IMAGE) {
            float tabW = 25 * rate;
            float contentX = tabW + 15 * rate;
            float contentY = 60 * rate;
            List<File> images = mainActivity.getImportedImages();
            for (int i = 0; i < images.size(); i++) {
                float fy = contentY + i * 26 * rate;
                if (x > contentX && x < sidebarW - 10 * rate
					&& y > fy - 10 && y < fy + 16) {
                    imageDownTime = System.currentTimeMillis();
                    draggingImageName = images.get(i).getName();
                    dragImageX = x;
                    dragImageY = y;
                    isDraggingImage = false;
                    return true;
                }
            }
        }

        if (e.getAction() == MotionEvent.ACTION_MOVE && draggingImageName != null) {
            if (!isDraggingImage) {
                if (System.currentTimeMillis() - imageDownTime > DRAG_DELAY) {
                    isDraggingImage = true;
                    Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    if (vib != null) vib.vibrate(100);
                    safeInvalidate();
                }
            } else {
                dragImageX = x;
                dragImageY = y;
                safeInvalidate();
            }
            return true;
        }

        if (e.getAction() == MotionEvent.ACTION_UP && isDraggingImage && draggingImageName != null) {
            float wx = (x - EngineData.scrollX) / EngineData.scale;
            float wy = (y - EngineData.scrollY) / EngineData.scale;
            for (int i = 0; i < EngineData.nodeList.size(); i++) {
                GameNode nd = EngineData.nodeList.get(i);
                if (wx >= nd.x && wx <= nd.x + nd.w && wy >= nd.y && wy <= nd.y + nd.h) {
                    Module m = EngineData.modMap.get(nd.id);
                    if (m != null) {
                        m.bgImageName = draggingImageName;
                        Toast.makeText(getContext(), "已设置背景图: " + draggingImageName, Toast.LENGTH_SHORT).show();
                        EngineData.lineDirty = true;
                        safeInvalidate();
                    }
                    break;
                }
            }
            isDraggingImage = false;
            draggingImageName = null;
            safeInvalidate();
            return true;
        }

        return handleEditorTouch(e);
    }

    boolean handleSidebarTouch(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        float sidebarW = SIDEBAR_W * rate;
        float tabW = 25 * rate;
        float tabH = sH / 3f;

        if (e.getAction() == MotionEvent.ACTION_UP) {
            if (x < tabW) {
                int tab = (int)(y / tabH);
                if (tab >= 0 && tab < 3 && tab != currentTab) {
                    currentTab = tab;
                    safeInvalidate();
                }
                return true;
            }

            if (currentTab == TAB_FUNC) {
                float contentX = tabW + 10 * rate;
                float contentY = 60 * rate;
                String[] funcs = {"创建节点", "创建变量", "返回主页", "查看介绍", "项目总览", "变量管理"};
                for (int i = 0; i < funcs.length; i++) {
                    float fy = contentY + i * 38 * rate;
                    if (x > contentX && x < sidebarW - 10 * rate
						&& y > fy - 10 && y < fy + 25) {
                        if (i == 0) createNodeAtCenter();
                        else if (i == 1) mainActivity.showCreateVarDialog();
                        else if (i == 2) { pageScene = 0; safeInvalidate(); }
                        else if (i == 3) Toast.makeText(getContext(), "Galgame 编辑器 v2.0", Toast.LENGTH_SHORT).show();
                        else if (i == 4) { pageScene = 2; safeInvalidate(); }
                        else if (i == 5) mainActivity.showVarManageDialog();
                        return true;
                    }
                }
                return true;
            }

            if (currentTab == TAB_IMAGE || currentTab == TAB_AUDIO) {
                List<File> items = (currentTab == TAB_IMAGE) ? mainActivity.getImportedImages()
					: mainActivity.getImportedAudios();
                float contentX = tabW + 10 * rate;
                float contentY = 55 * rate;
                for (int i = 0; i < items.size(); i++) {
                    float fy = contentY + i * 26 * rate;
                    if (x > contentX && x < sidebarW - 10 * rate
						&& y > fy - 10 && y < fy + 16) {
                        final File f = items.get(i);
                        showResourceDialog(f);
                        return true;
                    }
                }
            }
        }
        return true;
    }

    void showResourceDialog(final File f) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(f.getName());
        builder.setItems(new String[]{"重命名", "删除", "复制"},
            new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    if (which == 0) mainActivity.showRenameDialog(f);
                    else if (which == 1) {
                        if (f.delete()) {
                            Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                            safeInvalidate();
                        }
                    } else if (which == 2) {
                        try {
                            File parent = f.getParentFile();
                            String name = f.getName();
                            int dot = name.lastIndexOf(".");
                            String base = (dot > 0) ? name.substring(0, dot) : name;
                            String ext = (dot > 0) ? name.substring(dot) : "";
                            File copy = new File(parent, base + "_副本" + ext);
                            FileInputStream fis = new FileInputStream(f);
                            FileOutputStream fos = new FileOutputStream(copy);
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = fis.read(buf)) != -1) fos.write(buf, 0, len);
                            fis.close();
                            fos.close();
                            Toast.makeText(getContext(), "已复制", Toast.LENGTH_SHORT).show();
                            safeInvalidate();
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "复制失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    boolean handleRunTouch(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (x >= 20*rate && x <= 120*rate && y >= 20*rate && y <= 70*rate) {
                    pageScene = 0;
                    stopBgm();
                    stopIdleTimer();
                    safeInvalidate();
                    return true;
                }
                touchDownY = y;
                isTouchOptArea = x > sW/2 && y < sH - 180*rate;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isTouchOptArea) {
                    float dy = y - touchDownY;
                    optScrollY += dy;
                    optScrollY = Math.max(optScrollY, -2000*rate);
                    optScrollY = Math.min(optScrollY, 0);
                    touchDownY = y;
                    safeInvalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (EngineData.runIsEnd) return true;
                float optY = 70*rate + optScrollY;
                for (int i = 0; i < EngineData.runOptList.size(); i++) {
                    OptionItem opt = EngineData.runOptList.get(i);
                    if (x >= sW/2 + 20*rate && x <= sW - 20*rate
						&& y >= optY && y <= optY + 56*rate) {
                        EngineData.execMultiVarAction(opt.varActionStr);
                        List<String> showList = new ArrayList<String>();
                        List<String> hideList = new ArrayList<String>();
                        EngineData.parseShowHideVar(opt.showVarStr, showList);
                        EngineData.parseShowHideVar(opt.hideVarStr, hideList);
                        for (int j = 0; j < showList.size(); j++) {
                            if (!EngineData.showTopVar.contains(showList.get(j)))
                                EngineData.showTopVar.add(showList.get(j));
                        }
                        for (int j = 0; j < hideList.size(); j++) {
                            EngineData.showTopVar.remove(hideList.get(j));
                        }
                        if ("保存游戏".equals(opt.optText)) {
                            mainActivity.doSaveGame();
                        } else {
                            EngineData.runNowId = opt.jumpId;
                            EngineData.refreshRunData();
                            startIdleTimer();
                            playNodeBgm();
                        }
                        safeInvalidate();
                        break;
                    }
                    optY += 66*rate;
                }
                break;
        }
        safeInvalidate();
        return true;
    }

    boolean handleOverviewTouch(MotionEvent e) {
		float x = e.getX(), y = e.getY();

		if (e.getAction() == MotionEvent.ACTION_UP) {
			float yStart = 100 * rate;
			float yRow = yStart + 50 * rate;

			// ---- 返回按钮（右下角） ----
			float backX = sW - 160 * rate;
			float backY = sH - 70 * rate;
			if (x >= backX && x <= backX + 120*rate && y >= backY && y <= backY + 50*rate) {
				pageScene = 0;
				safeInvalidate();
				return true;
			}

			// ---- 项目操作按钮 ----
			String[] actions = {"切换项目", "新建项目", "重命名项目", "删除项目", "复制项目"};
			for (int i = 0; i < actions.length; i++) {
				float bx = 40 * rate + i * (130 * rate + 10 * rate);
				if (x >= bx && x <= bx + 130 * rate && y >= yRow && y <= yRow + 38 * rate) {
					if (i == 0) mainActivity.switchProject();
					else if (i == 1) mainActivity.createNewProject();
					else if (i == 2) mainActivity.renameCurrentProject();
					else if (i == 3) mainActivity.deleteCurrentProject();
					else if (i == 4) mainActivity.copyCurrentProject();
					safeInvalidate();
					return true;
				}
			}

			// ---- 存档操作按钮 ----
			float ySave = yRow + 50 * rate + 30 * rate;
			String[] saveActions = {"切换存档", "新建存档", "重命名存档", "删除存档", "复制存档"};
			for (int i = 0; i < saveActions.length; i++) {
				float bx = 40 * rate + i * (130 * rate + 10 * rate);
				if (x >= bx && x <= bx + 130 * rate && y >= ySave && y <= ySave + 38 * rate) {
					if (i == 0) mainActivity.switchSave();
					else if (i == 1) mainActivity.createNewSave();
					else if (i == 2) mainActivity.renameCurrentSave();
					else if (i == 3) mainActivity.deleteCurrentSave();
					else if (i == 4) mainActivity.copyCurrentSave();
					safeInvalidate();
					return true;
				}
			}
		}

		return true;
	}

    boolean handleEditorTouch(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        float wx = (x - EngineData.scrollX) / EngineData.scale;
        float wy = (y - EngineData.scrollY) / EngineData.scale;

        if (e.getAction() == MotionEvent.ACTION_UP) {
            float topY = 10*rate, btnW = 72*rate, btnH = 40*rate;
            String[] btns = {"项目", "运行", "保存", "存档", "导入", "设置"};
            for (int i = 0; i < btns.length; i++) {
                float bx = sW - (btns.length * (btnW + 8*rate)) - 16*rate + i * (btnW + 8*rate);
                if (x >= bx && x <= bx + btnW && y >= topY && y <= topY + btnH) {
                    if (i == 0) { pageScene = 2; safeInvalidate(); return true; }
                    else if (i == 1) { startGameRun(); return true; }
                    else if (i == 2) { mainActivity.saveProject(); return true; }
                    else if (i == 3) { mainActivity.showSaveListDialog(); return true; }
                    else if (i == 4) { mainActivity.showImportDialog(); return true; }
                    else if (i == 5) { mainActivity.showStorageSettings(); return true; }
                }
            }
        }

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            downX = x; downY = y;
            EngineData.selectedLinePoint = null;

            List<LinePoint> collide = findCollidePoints(wx, wy);
            if (collide.size() > 0) {
                EngineData.selectedLinePoint = collide.get(0);
                safeInvalidate();
                return true;
            }

            EngineData.bottomLineStart = hitBottomPort(wx, wy);
            if (EngineData.bottomLineStart != null) {
                EngineData.drawBottomLine = true;
                EngineData.lineTouchX = wx; EngineData.lineTouchY = wy;
                return true;
            }
            EngineData.topLineStart = hitTopPort(wx, wy);
            if (EngineData.topLineStart != null) {
                EngineData.drawTopLine = true;
                EngineData.lineTouchX = wx; EngineData.lineTouchY = wy;
                return true;
            }
            EngineData.lineStart = hitOutPort(wx, wy);
            if (EngineData.lineStart != null) {
                EngineData.drawLine = true;
                EngineData.lineTouchX = wx; EngineData.lineTouchY = wy;
                return true;
            }

            boolean hit = false;
            for (int i = 0; i < EngineData.nodeList.size(); i++) {
                GameNode nd = EngineData.nodeList.get(i);
                float b1x = nd.x + nd.w - 2*nd.btnW;
                if (wx >= b1x && wx <= b1x + nd.btnW && wy >= nd.y && wy <= nd.y + nd.h) {
                    EngineData.selectId = nd.id; EngineData.dragBtn = 1; hit = true; break;
                }
                float b2x = nd.x + nd.w - nd.btnW;
                if (wx >= b2x && wx <= b2x + nd.btnW && wy >= nd.y && wy <= nd.y + nd.h) {
                    EngineData.selectId = nd.id; EngineData.dragBtn = 2; hit = true; break;
                }
                if (wx >= nd.x && wx <= nd.x + nd.w && wy >= nd.y && wy <= nd.y + nd.h) {
                    EngineData.selectId = nd.id; dragNode = true; lastX = x; lastY = y; hit = true; break;
                }
            }
            if (!hit) { lastX = x; lastY = y; }
            safeInvalidate();
        }

        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = x - lastX, dy = y - lastY;
            if (Math.abs(dx) < TOUCH_SLOP && Math.abs(dy) < TOUCH_SLOP) return true;

            if (EngineData.drawLine || EngineData.drawTopLine || EngineData.drawBottomLine) {
                EngineData.lineTouchX = wx; EngineData.lineTouchY = wy;
                safeInvalidate(); return true;
            }

            if (dragNode && EngineData.selectId != -1) {
                for (int i = 0; i < EngineData.nodeList.size(); i++) {
                    GameNode nd = EngineData.nodeList.get(i);
                    if (nd.id == EngineData.selectId) {
                        nd.x = snap(nd.x + dx / EngineData.scale);
                        nd.y = snap(nd.y + dy / EngineData.scale);
                        break;
                    }
                }
                lastX = x; lastY = y;
                EngineData.lineDirty = true;
                safeInvalidate();
            } else {
                EngineData.scrollX += dx; EngineData.scrollY += dy;
                float maxScroll = 2000f * EngineData.scale;
                EngineData.scrollX = Math.max(-maxScroll, Math.min(EngineData.scrollX, maxScroll));
                EngineData.scrollY = Math.max(-maxScroll, Math.min(EngineData.scrollY, maxScroll));
                lastX = x; lastY = y;
                safeInvalidate();
            }
        }

        if (e.getAction() == MotionEvent.ACTION_UP) {
            if (EngineData.drawLine) {
                GameNode in = hitInPort(wx, wy);
                if (in != null && EngineData.lineStart.id != in.id) {
                    if (!EngineData.hasSameLine(EngineData.lineStart.id, in.id, LinePoint.TYPE_YELLOW)) {
                        Module m = EngineData.modMap.get(EngineData.lineStart.id);
                        if (m != null) {
                            OptionItem o = new OptionItem();
                            o.jumpId = in.id;
                            o.optText = "选项" + (m.optionList.size() + 1);
                            m.optionList.add(o);
                            EngineData.lineDirty = true;
                        }
                    }
                }
                EngineData.drawLine = false; EngineData.lineStart = null;
                safeInvalidate();
            }
            if (EngineData.drawTopLine) {
                GameNode in = hitInPort(wx, wy);
                if (in != null && EngineData.topLineStart.id != in.id) {
                    Module m = EngineData.modMap.get(EngineData.topLineStart.id);
                    if (m != null) { m.enterJumpId = in.id; EngineData.lineDirty = true; }
                }
                EngineData.drawTopLine = false; EngineData.topLineStart = null;
                safeInvalidate();
            }
            if (EngineData.drawBottomLine) {
                GameNode in = hitInPort(wx, wy);
                if (in != null && EngineData.bottomLineStart.id != in.id) {
                    Module m = EngineData.modMap.get(EngineData.bottomLineStart.id);
                    if (m != null) { m.greenJumpId = in.id; EngineData.lineDirty = true; }
                }
                EngineData.drawBottomLine = false; EngineData.bottomLineStart = null;
                safeInvalidate();
            }

            if (EngineData.selectId != -1 && EngineData.dragBtn > 0 && !dragNode) {
                Module m = EngineData.modMap.get(EngineData.selectId);
                if (m != null) {
                    if (EngineData.dragBtn == 1) mainActivity.showNodeEditDialog(m);
                    else if (EngineData.dragBtn == 2) {
                        EngineData.modMap.remove(EngineData.selectId);
                        for (int i = 0; i < EngineData.nodeList.size(); i++) {
                            if (EngineData.nodeList.get(i).id == EngineData.selectId) {
                                EngineData.nodeList.remove(i); break;
                            }
                        }
                        EngineData.lineDirty = true;
                        EngineData.selectId = -1;
                        safeInvalidate();
                    }
                }
            }
            dragNode = false; EngineData.dragBtn = -1;
            safeInvalidate();
        }
        return true;
    }

    void createNodeAtCenter() {
        int id = EngineData.getEmptyId();
        Module m = new Module(id);
        m.defaultStory = "新节点";
        m.displayName = "节点" + id;
        float cx = -EngineData.scrollX / EngineData.scale + sW / 2 / EngineData.scale;
        float cy = -EngineData.scrollY / EngineData.scale + sH / 2 / EngineData.scale;
        GameNode nd = new GameNode(id, cx - 260*rate, cy - 90*rate, rate);
        EngineData.nodeList.add(nd);
        EngineData.lineDirty = true;
        safeInvalidate();
        Toast.makeText(getContext(), "已创建节点 " + id, Toast.LENGTH_SHORT).show();
    }

    GameNode hitOutPort(float x, float y) {
        for (int i = 0; i < EngineData.nodeList.size(); i++) {
            GameNode nd = EngineData.nodeList.get(i);
            nd.refreshPos();
            if (Math.abs(x - nd.outX) < 40 && Math.abs(y - nd.outY) < 40) return nd;
        }
        return null;
    }
    GameNode hitTopPort(float x, float y) {
        for (int i = 0; i < EngineData.nodeList.size(); i++) {
            GameNode nd = EngineData.nodeList.get(i);
            nd.refreshPos();
            if (Math.abs(x - nd.topOutX) < 40 && Math.abs(y - nd.topOutY) < 40) return nd;
        }
        return null;
    }
    GameNode hitBottomPort(float x, float y) {
        for (int i = 0; i < EngineData.nodeList.size(); i++) {
            GameNode nd = EngineData.nodeList.get(i);
            nd.refreshPos();
            if (Math.abs(x - nd.bottomOutX) < 40 && Math.abs(y - nd.bottomOutY) < 40) return nd;
        }
        return null;
    }
    GameNode hitInPort(float x, float y) {
        for (int i = 0; i < EngineData.nodeList.size(); i++) {
            GameNode nd = EngineData.nodeList.get(i);
            nd.refreshPos();
            if (Math.abs(x - nd.inX) < 40 && Math.abs(y - nd.inY) < 40) return nd;
        }
        return null;
    }
    List<LinePoint> findCollidePoints(float wx, float wy) {
        List<LinePoint> res = new ArrayList<LinePoint>();
        for (int i = 0; i < EngineData.linePoints.size(); i++) {
            LinePoint lp = EngineData.linePoints.get(i);
            float dx = wx - lp.cx, dy = wy - lp.cy;
            if (Math.sqrt(dx*dx + dy*dy) <= touchR * rate) res.add(lp);
        }
        return res;
    }

    void startGameRun() {
        if (EngineData.nodeList.size() == 0) {
            Toast.makeText(getContext(), "请先添加节点", Toast.LENGTH_SHORT).show();
            return;
        }
        EngineData.runNowId = EngineData.nodeList.get(0).id;
        EngineData.refreshRunData();
        pageScene = 1;
        optScrollY = 0;
        startIdleTimer();
        playNodeBgm();
        safeInvalidate();
    }

    void startIdleTimer() {
        stopIdleTimer();
        if (!EngineData.modMap.containsKey(EngineData.runNowId)) return;
        Module cur = EngineData.modMap.get(EngineData.runNowId);
        if (cur.greenJumpId == -1 || cur.idleTimeMs <= 0) return;
        isIdleWaiting = true;
        idleTimer = new Timer();
        idleTimer.schedule(new TimerTask() {
				public void run() {
					mainActivity.runOnUiThread(new Runnable() {
							public void run() {
								if (isIdleWaiting && EngineData.runNowId != -1) {
									Module m = EngineData.modMap.get(EngineData.runNowId);
									if (m != null && m.greenJumpId != -1) {
										EngineData.runNowId = m.greenJumpId;
										EngineData.refreshRunData();
										playNodeBgm();
										safeInvalidate();
									}
								}
							}
						});
				}
			}, cur.idleTimeMs);
    }

    void stopIdleTimer() {
        isIdleWaiting = false;
        if (idleTimer != null) { idleTimer.cancel(); idleTimer = null; }
    }

    void playNodeBgm() {
        if (!EngineData.modMap.containsKey(EngineData.runNowId)) return;
        Module cur = EngineData.modMap.get(EngineData.runNowId);
        if (cur.bgmName != null && cur.bgmName.length() > 0) {
            File f = mainActivity.getAudioFile(cur.bgmName);
            if (f != null && f.exists()) {
                try {
                    if (bgmPlayer != null) { bgmPlayer.release(); bgmPlayer = null; }
                    bgmPlayer = new MediaPlayer();
                    bgmPlayer.setDataSource(f.getAbsolutePath());
                    bgmPlayer.prepare();
                    bgmPlayer.setLooping(true);
                    bgmPlayer.start();
                } catch (Exception e) { e.printStackTrace(); }
            }
        } else {
            stopBgm();
        }
    }

    void stopBgm() {
        if (bgmPlayer != null) {
            try { bgmPlayer.stop(); bgmPlayer.release(); } catch (Exception e) {}
            bgmPlayer = null;
        }
    }
}

// ============================================================
// MainActivity
// ============================================================

public class MainActivity extends Activity {
    DrawCanvas drawCanvas;
    private String storageRoot = "";
    private static final String PREFS_NAME = "GalbianjiPrefs";
    private static final String KEY_STORAGE = "storage_path";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        loadStoragePath();
        initTestDataIfEmpty();
        drawCanvas = new DrawCanvas(this);
        setContentView(drawCanvas);
        drawCanvas.post(new Runnable() {
            public void run() { drawCanvas.safeInvalidate(); }
        });
        handleIntent(getIntent());
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action) && intent.getType() != null) {
            if (intent.getType().startsWith("image/")) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null) importImageFromUri(uri);
            } else if ("text/plain".equals(intent.getType())) {
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (text != null && text.length() > 0) showImportTextDialog(text);
            }
        }
    }

    void showImportTextDialog(final String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText et = new EditText(this);
        et.setText(text);
        et.setMinLines(5);
        builder.setView(et);
        builder.setTitle("导入文本");
        builder.setPositiveButton("导入", new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface dialog, int which) {
                String content = et.getText().toString();
                if (content.length() > 0) {
                    int id = EngineData.getEmptyId();
                    Module m = new Module(id);
                    m.defaultStory = content;
                    m.displayName = "导入文本";
                    GameNode node = new GameNode(id, 500 + EngineData.nodeList.size()*100, 300, 1f);
                    EngineData.nodeList.add(node);
                    EngineData.lineDirty = true;
                    drawCanvas.safeInvalidate();
                    Toast.makeText(MainActivity.this, "导入成功", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    void importImageFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            if (bmp != null) {
                String name = "img_" + System.currentTimeMillis() + ".png";
                File dir = new File(getProjectMediaDir(), "images");
                if (!dir.exists()) dir.mkdirs();
                File dest = new File(dir, name);
                FileOutputStream fos = new FileOutputStream(dest);
                bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.close();
                Toast.makeText(this, "图片已导入: " + name, Toast.LENGTH_SHORT).show();
                drawCanvas.safeInvalidate();
            }
        } catch (Exception e) { e.printStackTrace(); Toast.makeText(this, "导入失败", Toast.LENGTH_SHORT).show(); }
    }

    void loadStoragePath() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        storageRoot = prefs.getString(KEY_STORAGE, "");
        if (storageRoot == null || storageRoot.length() == 0) {
            storageRoot = getDefaultStoragePath();
        }
        new File(storageRoot).mkdirs();
    }

    String getDefaultStoragePath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath() + "/galgamebianjiqi";
        } else {
            return getFilesDir().getAbsolutePath() + "/galgamebianjiqi";
        }
    }

    String getCurrentProjectPath() {
        return storageRoot + "/projects";
    }

    File getProjectMediaDir() {
        return new File(getCurrentProjectPath());
    }

    void initTestDataIfEmpty() {
        if (EngineData.nodeList.size() == 0) {
            EngineData.clearAllData();
            int id1 = EngineData.getEmptyId();
            Module m1 = new Module(id1);
            m1.defaultStory = "欢迎使用 Galgame 编辑器！\n点击右上角【运行】开始游戏";
            m1.displayName = "开始";
            EngineData.nodeList.add(new GameNode(id1, 200, 300, 1f));
            int id2 = EngineData.getEmptyId();
            Module m2 = new Module(id2);
            m2.defaultStory = "第二个节点\n你已经成功跳转到这里了！";
            m2.displayName = "节点2";
            m2.enterJumpId = id1;
            EngineData.nodeList.add(new GameNode(id2, 900, 300, 1f));
            OptionItem opt = new OptionItem();
            opt.optText = "跳转到节点2";
            opt.jumpId = id2;
            m1.optionList.add(opt);
            EngineData.lineDirty = true;
            EngineData.currentProjectName = "示例项目";
        }
    }

    // ============================================================
    // 节点编辑
    // ============================================================

    public void showNodeEditDialog(final Module mod) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final ScrollView sv = new ScrollView(this);
        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);

        final EditText etName = new EditText(this);
        etName.setHint("显示名称");
        etName.setText(mod.displayName);
        layout.addView(etName);

        final EditText etBgm = new EditText(this);
        etBgm.setHint("背景音乐文件名");
        etBgm.setText(mod.bgmName);
        layout.addView(etBgm);

        final TextView tvBg = new TextView(this);
        tvBg.setText("当前背景图: " + (mod.bgImageName != null && mod.bgImageName.length() > 0 ? mod.bgImageName : "无"));
        tvBg.setTextColor(Color.WHITE);
        layout.addView(tvBg);

        Button btnSelectBg = new Button(this);
        btnSelectBg.setText("选择背景图");
        btnSelectBg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final List<File> images = getImportedImages();
                if (images.size() == 0) {
                    Toast.makeText(MainActivity.this, "请先导入图片", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] names = new String[images.size()];
                for (int i = 0; i < images.size(); i++) {
                    names[i] = images.get(i).getName();
                }
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("选择背景图")
                    .setItems(names, new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface dialog, int which) {
                            mod.bgImageName = images.get(which).getName();
                            tvBg.setText("当前背景图: " + mod.bgImageName);
                            Toast.makeText(MainActivity.this, "已选择: " + mod.bgImageName, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            }
        });
        layout.addView(btnSelectBg);

        Button btnClearBg = new Button(this);
        btnClearBg.setText("清除背景图");
        btnClearBg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mod.bgImageName = "";
                tvBg.setText("当前背景图: 无");
                Toast.makeText(MainActivity.this, "已清除背景图", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnClearBg);

        final EditText etStory = new EditText(this);
        etStory.setHint("默认剧情");
        etStory.setText(mod.defaultStory);
        etStory.setMinLines(3);
        layout.addView(etStory);

        final EditText etCond = new EditText(this);
        etCond.setHint("进入条件");
        etCond.setText(mod.enterCondLine);
        layout.addView(etCond);

        final EditText etIdle = new EditText(this);
        etIdle.setHint("闲置跳转时间(ms)");
        etIdle.setText(String.valueOf(mod.idleTimeMs));
        layout.addView(etIdle);

        final CheckBox cbCond = new CheckBox(this);
        cbCond.setText("启用进入条件");
        cbCond.setChecked(mod.useEnterCond);
        layout.addView(cbCond);

        final CheckBox cbEnd = new CheckBox(this);
        cbEnd.setText("是结局节点");
        cbEnd.setChecked(mod.isEndNode);
        layout.addView(cbEnd);

        Button btnOptions = new Button(this);
        btnOptions.setText("管理选项(" + mod.optionList.size() + ")");
        btnOptions.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showOptionListDialog(mod); }
        });
        layout.addView(btnOptions);

        Button btnCopy = new Button(this);
        btnCopy.setText("复制此模块");
        btnCopy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { copyModule(mod); }
        });
        layout.addView(btnCopy);

        sv.addView(layout);
        builder.setView(sv);
        builder.setTitle("编辑节点 " + mod.id);
        builder.setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface dialog, int which) {
                mod.displayName = etName.getText().toString();
                mod.bgmName = etBgm.getText().toString();
                mod.defaultStory = etStory.getText().toString();
                mod.enterCondLine = etCond.getText().toString();
                mod.useEnterCond = cbCond.isChecked();
                mod.isEndNode = cbEnd.isChecked();
                try { mod.idleTimeMs = Long.parseLong(etIdle.getText().toString()); }
                catch (Exception e) { mod.idleTimeMs = 3000; }
                EngineData.lineDirty = true;
                drawCanvas.safeInvalidate();
                Toast.makeText(MainActivity.this, "已保存", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    void showOptionListDialog(final Module mod) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final ListView lv = new ListView(this);

		// 构建完整列表：蓝线 + 绿线 + 普通选项
		final List<String> displayList = new ArrayList<String>();
		final List<Integer> typeList = new ArrayList<Integer>(); // 0=蓝线, 1=绿线, 2=普通选项
		final List<Integer> indexList = new ArrayList<Integer>(); // 普通选项的索引

		// 1. 蓝线（进入条件跳转）
		if (mod.enterJumpId != -1) {
			displayList.add("🔵 [蓝线] 进入条件跳转 -> 节点 " + mod.enterJumpId);
			typeList.add(0);
			indexList.add(-1);
		}

		// 2. 绿线（闲置跳转）
		if (mod.greenJumpId != -1) {
			displayList.add("🟢 [绿线] 闲置跳转(" + mod.idleTimeMs + "ms) -> 节点 " + mod.greenJumpId);
			typeList.add(1);
			indexList.add(-1);
		}

		// 3. 普通选项（黄线）
		for (int i = 0; i < mod.optionList.size(); i++) {
			OptionItem o = mod.optionList.get(i);
			displayList.add("🟡 " + o.optText + " -> 节点 " + o.jumpId);
			typeList.add(2);
			indexList.add(i);
		}

		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {
			public int getCount() { return displayList.size(); }
			public String getItem(int pos) { return displayList.get(pos); }
		};
		lv.setAdapter(adapter);

		builder.setTitle("连线管理（点击编辑/删除）");
		builder.setView(lv);

		builder.setPositiveButton("添加选项", new android.content.DialogInterface.OnClickListener() {
				public void onClick(android.content.DialogInterface dialog, int which) {
					showAddOptionDialog(mod);
				}
			});

		// 点击 = 编辑
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
					int type = typeList.get(pos);
					if (type == 0) {
						// 编辑蓝线
						showEditBlueLineDialog(mod);
					} else if (type == 1) {
						// 编辑绿线
						showEditGreenLineDialog(mod);
					} else {
						// 编辑普通选项
						int idx = indexList.get(pos);
						if (idx >= 0 && idx < mod.optionList.size()) {
							showEditOptionDialog(mod, idx);
						}
					}
				}
			});

		// 长按 = 删除
		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
					final int type = typeList.get(pos);
					final int idx = indexList.get(pos);
					String title = "";
					if (type == 0) title = "删除蓝线（进入条件跳转）";
					else if (type == 1) title = "删除绿线（闲置跳转）";
					else title = "删除选项 \"" + mod.optionList.get(idx).optText + "\"";

					new AlertDialog.Builder(MainActivity.this)
						.setTitle("删除")
						.setMessage("确定" + title + " 吗？")
						.setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
							public void onClick(android.content.DialogInterface dialog, int which) {
								if (type == 0) {
									mod.enterJumpId = -1;
								} else if (type == 1) {
									mod.greenJumpId = -1;
								} else if (idx >= 0 && idx < mod.optionList.size()) {
									mod.optionList.remove(idx);
								}
								EngineData.lineDirty = true;
								drawCanvas.safeInvalidate();
								showOptionListDialog(mod);
								Toast.makeText(MainActivity.this, "已删除", Toast.LENGTH_SHORT).show();
							}
						})
						.setNegativeButton("取消", null)
						.show();
					return true;
				}
			});

		builder.setNegativeButton("关闭", null);
		builder.show();
	}
	void showEditBlueLineDialog(final Module mod) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(30, 30, 30, 30);

		final EditText etJump = new EditText(this);
		etJump.setHint("目标节点ID");
		etJump.setText(mod.enterJumpId != -1 ? String.valueOf(mod.enterJumpId) : "");
		layout.addView(etJump);

		final CheckBox cbUseCond = new CheckBox(this);
		cbUseCond.setText("启用进入条件");
		cbUseCond.setChecked(mod.useEnterCond);
		layout.addView(cbUseCond);

		final EditText etCond = new EditText(this);
		etCond.setHint("进入条件（多条件用,分隔）");
		etCond.setText(mod.enterCondLine);
		layout.addView(etCond);

		builder.setView(layout);
		builder.setTitle("编辑蓝线（进入条件跳转）");
		builder.setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
				public void onClick(android.content.DialogInterface dialog, int which) {
					try {
						mod.enterJumpId = Integer.parseInt(etJump.getText().toString());
					} catch (Exception e) {
						mod.enterJumpId = -1;
					}
					mod.useEnterCond = cbUseCond.isChecked();
					mod.enterCondLine = etCond.getText().toString();
					EngineData.lineDirty = true;
					drawCanvas.safeInvalidate();
					Toast.makeText(MainActivity.this, "蓝线已更新", Toast.LENGTH_SHORT).show();
				}
			});
		builder.setNegativeButton("取消", null);
		builder.show();
	}
	void showEditGreenLineDialog(final Module mod) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(30, 30, 30, 30);

		final EditText etJump = new EditText(this);
		etJump.setHint("目标节点ID");
		etJump.setText(mod.greenJumpId != -1 ? String.valueOf(mod.greenJumpId) : "");
		layout.addView(etJump);

		final EditText etIdle = new EditText(this);
		etIdle.setHint("闲置等待时间(毫秒)");
		etIdle.setText(String.valueOf(mod.idleTimeMs));
		layout.addView(etIdle);

		builder.setView(layout);
		builder.setTitle("编辑绿线（闲置跳转）");
		builder.setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
				public void onClick(android.content.DialogInterface dialog, int which) {
					try {
						mod.greenJumpId = Integer.parseInt(etJump.getText().toString());
					} catch (Exception e) {
						mod.greenJumpId = -1;
					}
					try {
						mod.idleTimeMs = Long.parseLong(etIdle.getText().toString());
					} catch (Exception e) {
						mod.idleTimeMs = 3000;
					}
					EngineData.lineDirty = true;
					drawCanvas.safeInvalidate();
					Toast.makeText(MainActivity.this, "绿线已更新", Toast.LENGTH_SHORT).show();
				}
			});
		builder.setNegativeButton("取消", null);
		builder.show();
	}

    void showAddOptionDialog(final Module mod) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);

        final EditText etText = new EditText(this);
        etText.setHint("选项文本");
        layout.addView(etText);
        final EditText etJump = new EditText(this);
        etJump.setHint("跳转节点ID");
        layout.addView(etJump);
        final EditText etCond = new EditText(this);
        etCond.setHint("显示条件");
        layout.addView(etCond);
        final EditText etVar = new EditText(this);
        etVar.setHint("变量动作");
        layout.addView(etVar);
        final CheckBox cbCond = new CheckBox(this);
        cbCond.setText("启用条件");
        layout.addView(cbCond);

        builder.setView(layout);
        builder.setTitle("添加选项");
        builder.setPositiveButton("添加", new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface dialog, int which) {
                OptionItem o = new OptionItem();
                o.optText = etText.getText().toString();
                o.useShowCond = cbCond.isChecked();
                o.showCondLine = etCond.getText().toString();
                o.varActionStr = etVar.getText().toString();
                try { o.jumpId = Integer.parseInt(etJump.getText().toString()); }
                catch (Exception e) { o.jumpId = -1; }
                if (o.optText.length() > 0 && o.jumpId != -1) {
                    mod.optionList.add(o);
                    EngineData.lineDirty = true;
                    drawCanvas.safeInvalidate();
                    Toast.makeText(MainActivity.this, "已添加", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    void showEditOptionDialog(final Module mod, final int pos) {
        final OptionItem o = mod.optionList.get(pos);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);

        final EditText etText = new EditText(this);
        etText.setText(o.optText);
        layout.addView(etText);
        final EditText etJump = new EditText(this);
        etJump.setText(String.valueOf(o.jumpId));
        layout.addView(etJump);
        final EditText etCond = new EditText(this);
        etCond.setText(o.showCondLine);
        layout.addView(etCond);
        final EditText etVar = new EditText(this);
        etVar.setText(o.varActionStr);
        layout.addView(etVar);
        final CheckBox cbCond = new CheckBox(this);
        cbCond.setChecked(o.useShowCond);
        layout.addView(cbCond);

        builder.setView(layout);
        builder.setTitle("编辑选项");
        builder.setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface dialog, int which) {
                o.optText = etText.getText().toString();
                o.useShowCond = cbCond.isChecked();
                o.showCondLine = etCond.getText().toString();
                o.varActionStr = etVar.getText().toString();
                try { o.jumpId = Integer.parseInt(etJump.getText().toString()); }
                catch (Exception e) { o.jumpId = -1; }
                EngineData.lineDirty = true;
                drawCanvas.safeInvalidate();
                Toast.makeText(MainActivity.this, "已更新", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("删除", new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface dialog, int which) {
                mod.optionList.remove(pos);
                EngineData.lineDirty = true;
                drawCanvas.safeInvalidate();
            }
        });
        builder.setNeutralButton("取消", null);
        builder.show();
    }

    void copyModule(Module src) {
        int id = EngineData.getEmptyId();
        Module copy = new Module(id);
        copy.displayName = src.displayName + "_副本";
        copy.defaultStory = src.defaultStory;
        copy.bgmName = src.bgmName;
        copy.bgImageName = src.bgImageName;
        copy.enterCondLine = src.enterCondLine;
        copy.useEnterCond = src.useEnterCond;
        copy.isEndNode = src.isEndNode;
        copy.endStory = src.endStory;
        copy.haveSaveOpt = src.haveSaveOpt;
        copy.idleTimeMs = src.idleTimeMs;
        copy.greenJumpId = src.greenJumpId;
        copy.enterJumpId = src.enterJumpId;
        for (int i = 0; i < src.condList.size(); i++) {
            CondItem ci = new CondItem();
            ci.condLine = src.condList.get(i).condLine;
            ci.storyText = src.condList.get(i).storyText;
            copy.condList.add(ci);
        }
        for (int i = 0; i < src.optionList.size(); i++) {
            OptionItem o = new OptionItem();
            o.optText = src.optionList.get(i).optText;
            o.jumpId = src.optionList.get(i).jumpId;
            o.useShowCond = src.optionList.get(i).useShowCond;
            o.showCondLine = src.optionList.get(i).showCondLine;
            o.varActionStr = src.optionList.get(i).varActionStr;
            o.showVarStr = src.optionList.get(i).showVarStr;
            o.hideVarStr = src.optionList.get(i).hideVarStr;
            copy.optionList.add(o);
        }
        EngineData.nodeList.add(new GameNode(id, src.id * 100 + 300, 400, 1f));
        EngineData.lineDirty = true;
        drawCanvas.safeInvalidate();
        Toast.makeText(this, "已复制节点 " + id, Toast.LENGTH_SHORT).show();
    }

    // ============================================================
    // 项目管理
    // ============================================================

    public void saveProject() {
        if (EngineData.nodeList.size() == 0) {
            Toast.makeText(this, "没有内容", Toast.LENGTH_SHORT).show();
            return;
        }
        File dir = new File(getCurrentProjectPath());
        if (!dir.exists()) dir.mkdirs();
        if (EngineData.currentProjectName == null || EngineData.currentProjectName.length() == 0) {
            showSaveAsDialog(dir);
            return;
        }
        saveProjectTo(dir, EngineData.currentProjectName);
    }

    void showSaveAsDialog(final File dir) {
        final EditText et = new EditText(this);
        et.setHint("项目名称");
        new AlertDialog.Builder(this)
            .setTitle("保存项目")
            .setView(et)
            .setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    String name = et.getText().toString();
                    if (name.length() == 0) name = "project_" + System.currentTimeMillis();
                    EngineData.currentProjectName = name;
                    saveProjectTo(dir, name);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    void saveProjectTo(File dir, String name) {
		try {
			File f = new File(dir, name + ".gbj");
			BufferedWriter w = new BufferedWriter(new FileWriter(f));
			w.write("#Galgame Project\n");
			w.write("PROJECT_NAME:" + name + "\n");
			w.write("NODE_COUNT:" + EngineData.nodeList.size() + "\n");
			w.write("VAR_COUNT:" + EngineData.varConfigList.size() + "\n");
			w.write("SCALE:" + EngineData.scale + "\n");
			w.write("SCROLL_X:" + EngineData.scrollX + "\n");
			w.write("SCROLL_Y:" + EngineData.scrollY + "\n\n");

			w.write("[NODES]\n");
			for (int i = 0; i < EngineData.nodeList.size(); i++) {
				GameNode nd = EngineData.nodeList.get(i);
				w.write(nd.id + "," + nd.x + "," + nd.y + "\n");
			}
			w.write("\n[MODULES]\n");

			Set<Integer> keys = EngineData.modMap.keySet();
			Integer[] arr = keys.toArray(new Integer[0]);
			for (int i = 0; i < arr.length; i++) {
				Module m = EngineData.modMap.get(arr[i]);
				w.write("MODULE:" + m.id + "\n");
				w.write("DISPLAY:" + m.displayName + "\n");
				w.write("BGM:" + m.bgmName + "\n");
				w.write("BGIMG:" + m.bgImageName + "\n");
				w.write("STORY:" + m.defaultStory.replace("\n", "\\n") + "\n");
				w.write("COND:" + m.enterCondLine + "\n");
				w.write("USE_COND:" + m.useEnterCond + "\n");
				w.write("IS_END:" + m.isEndNode + "\n");
				w.write("END_STORY:" + m.endStory.replace("\n", "\\n") + "\n");
				w.write("HAVE_SAVE:" + m.haveSaveOpt + "\n");
				w.write("IDLE:" + m.idleTimeMs + "\n");
				w.write("GREEN_JUMP:" + m.greenJumpId + "\n");
				w.write("BLUE_JUMP:" + m.enterJumpId + "\n");

				// 条件列表
				w.write("COND_LIST:" + m.condList.size() + "\n");
				for (int j = 0; j < m.condList.size(); j++) {
					CondItem ci = m.condList.get(j);
					w.write("  " + ci.condLine + "|" + ci.storyText.replace("\n", "\\n") + "\n");
				}

				// 选项列表（完整保存所有字段）
				w.write("OPTIONS:" + m.optionList.size() + "\n");
				for (int j = 0; j < m.optionList.size(); j++) {
					OptionItem o = m.optionList.get(j);
					w.write("  " + o.optText + "|" + o.jumpId + "|" + o.useShowCond + "|"
							+ o.showCondLine + "|" + o.varActionStr + "|" + o.showVarStr + "|" + o.hideVarStr + "\n");
				}
				w.write("END_MODULE\n");
			}

			// 变量配置
			w.write("\n[VARS]\n");
			for (int i = 0; i < EngineData.varConfigList.size(); i++) {
				GameVar gv = EngineData.varConfigList.get(i);
				w.write(gv.varName + "," + gv.baseValue + "," + gv.initShow + "," + gv.varType + "\n");
			}

			// 显示变量
			w.write("\n[SHOW_VARS]\n");
			for (int i = 0; i < EngineData.showTopVar.size(); i++) {
				w.write(EngineData.showTopVar.get(i) + "\n");
			}

			w.close();
			Toast.makeText(this, "已保存: " + f.getName(), Toast.LENGTH_SHORT).show();

		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

    public void loadProject() {
        File dir = new File(getCurrentProjectPath());
        if (!dir.exists() || dir.listFiles() == null) {
            Toast.makeText(this, "没有项目", Toast.LENGTH_SHORT).show();
            return;
        }
        final File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File d, String name) { return name.endsWith(".gbj"); }
        });
        if (files == null || files.length == 0) {
            Toast.makeText(this, "没有项目", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) names[i] = files[i].getName();
        new AlertDialog.Builder(this)
            .setTitle("选择项目")
            .setItems(names, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    loadProjectFile(files[which]);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    void loadProjectFile(File f) {
		try {
			EngineData.clearAllData();
			BufferedReader r = new BufferedReader(new FileReader(f));
			String line;
			Module cur = null;

			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) continue;

				if (line.startsWith("PROJECT_NAME:")) {
					EngineData.currentProjectName = line.substring(14);
				} else if (line.startsWith("SCALE:")) {
					try { EngineData.scale = Float.parseFloat(line.substring(6)); } catch (Exception e) {}
				} else if (line.startsWith("SCROLL_X:")) {
					try { EngineData.scrollX = Float.parseFloat(line.substring(9)); } catch (Exception e) {}
				} else if (line.startsWith("SCROLL_Y:")) {
					try { EngineData.scrollY = Float.parseFloat(line.substring(9)); } catch (Exception e) {}
				} else if (line.startsWith("MODULE:")) {
					int id = Integer.parseInt(line.substring(7));
					cur = new Module(id);
				} else if (cur != null) {
					if (line.startsWith("DISPLAY:")) {
						cur.displayName = line.substring(8);
					} else if (line.startsWith("BGM:")) {
						cur.bgmName = line.substring(4);
					} else if (line.startsWith("BGIMG:")) {
						cur.bgImageName = line.substring(6);
					} else if (line.startsWith("STORY:")) {
						cur.defaultStory = line.substring(6).replace("\\n", "\n");
					} else if (line.startsWith("COND:")) {
						cur.enterCondLine = line.substring(5);
					} else if (line.startsWith("USE_COND:")) {
						cur.useEnterCond = Boolean.parseBoolean(line.substring(9));
					} else if (line.startsWith("IS_END:")) {
						cur.isEndNode = Boolean.parseBoolean(line.substring(7));
					} else if (line.startsWith("END_STORY:")) {
						cur.endStory = line.substring(10).replace("\\n", "\n");
					} else if (line.startsWith("HAVE_SAVE:")) {
						cur.haveSaveOpt = Boolean.parseBoolean(line.substring(10));
					} else if (line.startsWith("IDLE:")) {
						cur.idleTimeMs = Long.parseLong(line.substring(5));
					} else if (line.startsWith("GREEN_JUMP:")) {
						cur.greenJumpId = Integer.parseInt(line.substring(11));
					} else if (line.startsWith("BLUE_JUMP:")) {
						cur.enterJumpId = Integer.parseInt(line.substring(10));
					} else if (line.startsWith("COND_LIST:")) {
						int cnt = Integer.parseInt(line.substring(10));
						for (int i = 0; i < cnt; i++) {
							String cl = r.readLine();
							if (cl != null && cl.trim().length() > 0) {
								String[] p = cl.trim().split("\\|");
								if (p.length >= 2) {
									CondItem ci = new CondItem();
									ci.condLine = p[0];
									ci.storyText = p[1].replace("\\n", "\n");
									cur.condList.add(ci);
								}
							}
						}
					} else if (line.startsWith("OPTIONS:")) {
						int cnt = Integer.parseInt(line.substring(8));
						for (int i = 0; i < cnt; i++) {
							String ol = r.readLine();
							if (ol != null && ol.trim().length() > 0) {
								String[] p = ol.trim().split("\\|", -1);
								if (p.length >= 7) {
									OptionItem o = new OptionItem();
									o.optText = p[0];
									o.jumpId = Integer.parseInt(p[1]);
									o.useShowCond = Boolean.parseBoolean(p[2]);
									o.showCondLine = p[3];
									o.varActionStr = p[4];
									o.showVarStr = p[5];
									o.hideVarStr = p[6];
									cur.optionList.add(o);
								}
							}
						}
					} else if (line.startsWith("END_MODULE")) {
						cur = null;
					}
				} else if (line.contains(",") && !line.startsWith("[")) {
					String[] p = line.split(",");
					if (p.length >= 3) {
						try {
							int id = Integer.parseInt(p[0]);
							float x = Float.parseFloat(p[1]);
							float y = Float.parseFloat(p[2]);
							EngineData.nodeList.add(new GameNode(id, x, y, 1f));
						} catch (Exception e) {}
					}
				}
			}
			r.close();

			EngineData.lineDirty = true;
			drawCanvas.safeInvalidate();
			Toast.makeText(this, "已加载项目: " + f.getName(), Toast.LENGTH_SHORT).show();

		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

    // ---- 项目操作 ----

    public void switchProject() {
        loadProject();
    }

    public void createNewProject() {
        EngineData.clearAllData();
        EngineData.currentProjectName = "新项目_" + System.currentTimeMillis();
        int id1 = EngineData.getEmptyId();
        Module m1 = new Module(id1);
        m1.defaultStory = "新项目开始";
        m1.displayName = "开始";
        EngineData.nodeList.add(new GameNode(id1, 200, 300, 1f));
        EngineData.lineDirty = true;
        drawCanvas.safeInvalidate();
        Toast.makeText(this, "已创建新项目", Toast.LENGTH_SHORT).show();
    }

    public void renameCurrentProject() {
        final EditText et = new EditText(this);
        et.setText(EngineData.currentProjectName);
        new AlertDialog.Builder(this)
            .setTitle("重命名项目")
            .setView(et)
            .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    String name = et.getText().toString();
                    if (name.length() > 0) {
                        File oldFile = new File(getCurrentProjectPath(), EngineData.currentProjectName + ".gbj");
                        File newFile = new File(getCurrentProjectPath(), name + ".gbj");
                        if (oldFile.exists()) {
                            oldFile.renameTo(newFile);
                        }
                        EngineData.currentProjectName = name;
                        drawCanvas.safeInvalidate();
                        Toast.makeText(MainActivity.this, "已重命名", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    public void deleteCurrentProject() {
        new AlertDialog.Builder(this)
            .setTitle("删除项目")
            .setMessage("确定删除当前项目吗？")
            .setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    File f = new File(getCurrentProjectPath(), EngineData.currentProjectName + ".gbj");
                    if (f.exists()) f.delete();
                    EngineData.clearAllData();
                    EngineData.currentProjectName = "";
                    drawCanvas.safeInvalidate();
                    Toast.makeText(MainActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    public void copyCurrentProject() {
        String name = EngineData.currentProjectName + "_副本";
        saveProjectTo(new File(getCurrentProjectPath()), name);
        Toast.makeText(this, "已复制项目", Toast.LENGTH_SHORT).show();
    }

    // ---- 存档操作 ----

    public void switchSave() {
        showSaveListDialog();
    }

    public void createNewSave() {
        doSaveGame();
    }

    public void renameCurrentSave() {
        File saveDir = new File(getCurrentProjectPath() + "/saves");
        final File[] saves = saveDir.listFiles(new FilenameFilter() {
            public boolean accept(File d, String name) { return name.endsWith(".sav"); }
        });
        if (saves == null || saves.length == 0) {
            Toast.makeText(this, "没有存档", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[saves.length];
        for (int i = 0; i < saves.length; i++) names[i] = saves[i].getName();
        new AlertDialog.Builder(this)
            .setTitle("选择要重命名的存档")
            .setItems(names, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    final File f = saves[which];
                    final EditText et = new EditText(MainActivity.this);
                    et.setText(f.getName().replace(".sav", ""));
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("重命名存档")
                        .setView(et)
                        .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                            public void onClick(android.content.DialogInterface dialog2, int which2) {
                                String name = et.getText().toString();
                                if (name.length() > 0) {
                                    File dest = new File(f.getParent(), name + ".sav");
                                    f.renameTo(dest);
                                    Toast.makeText(MainActivity.this, "已重命名", Toast.LENGTH_SHORT).show();
                                    drawCanvas.safeInvalidate();
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    public void deleteCurrentSave() {
        File saveDir = new File(getCurrentProjectPath() + "/saves");
        final File[] saves = saveDir.listFiles(new FilenameFilter() {
            public boolean accept(File d, String name) { return name.endsWith(".sav"); }
        });
        if (saves == null || saves.length == 0) {
            Toast.makeText(this, "没有存档", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[saves.length];
        for (int i = 0; i < saves.length; i++) names[i] = saves[i].getName();
        new AlertDialog.Builder(this)
            .setTitle("选择要删除的存档")
            .setItems(names, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    final File f = saves[which];
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("删除存档")
                        .setMessage("确定删除 " + f.getName() + " ?")
                        .setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                            public void onClick(android.content.DialogInterface dialog2, int which2) {
                                f.delete();
                                Toast.makeText(MainActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                                drawCanvas.safeInvalidate();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    public void copyCurrentSave() {
        File saveDir = new File(getCurrentProjectPath() + "/saves");
        final File[] saves = saveDir.listFiles(new FilenameFilter() {
            public boolean accept(File d, String name) { return name.endsWith(".sav"); }
        });
        if (saves == null || saves.length == 0) {
            Toast.makeText(this, "没有存档", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[saves.length];
        for (int i = 0; i < saves.length; i++) names[i] = saves[i].getName();
        new AlertDialog.Builder(this)
            .setTitle("选择要复制的存档")
            .setItems(names, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    File f = saves[which];
                    String name = f.getName();
                    int dot = name.lastIndexOf(".");
                    String base = (dot > 0) ? name.substring(0, dot) : name;
                    String ext = (dot > 0) ? name.substring(dot) : "";
                    File copy = new File(f.getParent(), base + "_副本" + ext);
                    try {
                        FileInputStream fis = new FileInputStream(f);
                        FileOutputStream fos = new FileOutputStream(copy);
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = fis.read(buf)) != -1) fos.write(buf, 0, len);
                        fis.close();
                        fos.close();
                        Toast.makeText(MainActivity.this, "已复制存档", Toast.LENGTH_SHORT).show();
                        drawCanvas.safeInvalidate();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "复制失败", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // ============================================================
    // 存档管理（原有）
    // ============================================================

    public int getSaveCount() {
        File dir = new File(getCurrentProjectPath() + "/saves");
        if (!dir.exists()) return 0;
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File d, String name) { return name.endsWith(".sav"); }
        });
        return files == null ? 0 : files.length;
    }

    public void doSaveGame() {
        File dir = new File(getCurrentProjectPath() + "/saves");
        if (!dir.exists()) dir.mkdirs();
        final EditText et = new EditText(this);
        et.setText("存档_" + System.currentTimeMillis());
        new AlertDialog.Builder(this)
            .setTitle("保存游戏")
            .setView(et)
            .setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    saveGameData(et.getText().toString());
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    void saveGameData(String name) {
        try {
            File f = new File(getCurrentProjectPath() + "/saves", name + ".sav");
            BufferedWriter w = new BufferedWriter(new FileWriter(f));
            w.write("NODE_ID:" + EngineData.runNowId + "\n");
            w.write("STORY:" + EngineData.runStory.replace("\n", "\\n") + "\n");
            w.write("IS_END:" + EngineData.runIsEnd + "\n");
            w.write("[VARS]\n");
            Set<String> keys = EngineData.gameVar.keySet();
            String[] arr = keys.toArray(new String[0]);
            for (int i = 0; i < arr.length; i++) {
                w.write(arr[i] + "," + EngineData.gameVar.get(arr[i]) + "\n");
            }
            w.write("[SHOW]\n");
            for (int i = 0; i < EngineData.showTopVar.size(); i++) {
                w.write(EngineData.showTopVar.get(i) + "\n");
            }
            w.close();
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void showSaveListDialog() {
        File dir = new File(getCurrentProjectPath() + "/saves");
        if (!dir.exists() || dir.listFiles() == null || dir.listFiles().length == 0) {
            Toast.makeText(this, "没有存档", Toast.LENGTH_SHORT).show();
            return;
        }
        final File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File d, String name) { return name.endsWith(".sav"); }
        });
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) names[i] = files[i].getName().replace(".sav", "");
        new AlertDialog.Builder(this)
            .setTitle("选择存档")
            .setItems(names, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    loadSaveFile(files[which]);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    void loadSaveFile(File f) {
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("NODE_ID:")) {
                    EngineData.runNowId = Integer.parseInt(line.substring(8));
                } else if (line.startsWith("STORY:")) {
                    EngineData.runStory = line.substring(6).replace("\\n", "\n");
                } else if (line.startsWith("IS_END:")) {
                    EngineData.runIsEnd = Boolean.parseBoolean(line.substring(7));
                } else if (line.contains(",") && !line.startsWith("[") && !line.startsWith("NODE") && !line.startsWith("STORY") && !line.startsWith("IS_END")) {
                    String[] p = line.split(",");
                    if (p.length >= 2) EngineData.gameVar.put(p[0], Integer.parseInt(p[1]));
                } else if (!line.startsWith("[") && !line.startsWith("NODE") && !line.startsWith("STORY") && !line.startsWith("IS_END")) {
                    if (!EngineData.showTopVar.contains(line)) EngineData.showTopVar.add(line);
                }
            }
            r.close();
            EngineData.refreshRunData();
            drawCanvas.pageScene = 1;
            drawCanvas.safeInvalidate();
            Toast.makeText(this, "已加载存档", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ============================================================
    // 导入
    // ============================================================

    public void showImportDialog() {
        new AlertDialog.Builder(this)
            .setTitle("导入")
            .setItems(new String[]{"导入项目", "导入图片", "导入音频"}, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    if (which == 0) loadProject();
                    else if (which == 1) pickFile("image/*");
                    else if (which == 2) pickFile("audio/*");
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    void pickFile(String type) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType(type);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "选择文件"), 1001);
    }

    protected void onActivityResult(int req, int res, Intent data) {
        if (req == 1001 && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String type = getContentResolver().getType(uri);
                if (type != null && type.startsWith("image/")) importImageFromUri(uri);
                else if (type != null && type.startsWith("audio/")) importAudioFromUri(uri);
            }
        }
    }

    void importAudioFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            String name = "audio_" + System.currentTimeMillis() + ".mp3";
            File dir = new File(getProjectMediaDir(), "audios");
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, name);
            FileOutputStream fos = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) fos.write(buf, 0, len);
            fos.close(); is.close();
            Toast.makeText(this, "音频已导入: " + name, Toast.LENGTH_SHORT).show();
            drawCanvas.safeInvalidate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ============================================================
    // 资源管理
    // ============================================================

    public File getImageFile(String name) {
		File f = new File(getProjectMediaDir() + "/images", name);
		return f.exists() ? f : null;
	}
	
	public List<File> getImportedImages() {
        List<File> list = new ArrayList<File>();
        File dir = new File(getProjectMediaDir(), "images");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) for (int i = 0; i < files.length; i++) list.add(files[i]);
        }
        return list;
    }

    public List<File> getImportedAudios() {
        List<File> list = new ArrayList<File>();
        File dir = new File(getProjectMediaDir(), "audios");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) for (int i = 0; i < files.length; i++) list.add(files[i]);
        }
        return list;
    }

    public File getAudioFile(String name) {
        File f = new File(getProjectMediaDir() + "/audios", name);
        return f.exists() ? f : null;
    }

    public void showRenameDialog(final File f) {
        final EditText et = new EditText(this);
        et.setText(f.getName());
        new AlertDialog.Builder(this)
            .setTitle("重命名")
            .setView(et)
            .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    String name = et.getText().toString();
                    if (name.length() > 0) {
                        File dest = new File(f.getParent(), name);
                        if (f.renameTo(dest)) {
                            Toast.makeText(MainActivity.this, "已重命名", Toast.LENGTH_SHORT).show();
                            drawCanvas.safeInvalidate();
                        }
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // ============================================================
    // 存储设置（增加应用包存储）
    // ============================================================

    public void showStorageSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);

        final RadioButton rbInternal = new RadioButton(this);
        rbInternal.setText("应用内部存储");
        final RadioButton rbPackage = new RadioButton(this);
        rbPackage.setText("应用包存储");
        final RadioButton rbExternal = new RadioButton(this);
        rbExternal.setText("外部存储(galgamebianjiqi)");

        rg.addView(rbInternal);
        rg.addView(rbPackage);
        rg.addView(rbExternal);

        if (storageRoot != null) {
            if (storageRoot.contains("/galgamebianjiqi") && !storageRoot.contains(getFilesDir().getAbsolutePath())) {
                rbExternal.setChecked(true);
            } else if (storageRoot.contains(getPackageName())) {
                rbPackage.setChecked(true);
            } else {
                rbInternal.setChecked(true);
            }
        } else {
            rbExternal.setChecked(true);
        }

        builder.setView(rg);
        builder.setTitle("存储位置");
        builder.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface dialog, int which) {
                String path;
                if (rbInternal.isChecked()) {
                    path = getFilesDir().getAbsolutePath() + "/galgamebianjiqi";
                } else if (rbPackage.isChecked()) {
                    path = getFilesDir().getAbsolutePath() + "/" + getPackageName() + "/galgamebianjiqi";
                } else {
                    path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/galgamebianjiqi";
                }
                new File(path).mkdirs();
                SharedPreferences.Editor ed = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                ed.putString(KEY_STORAGE, path);
                ed.commit();
                storageRoot = path;
                Toast.makeText(MainActivity.this, "已切换存储位置", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // ============================================================
    // 变量管理
    // ============================================================

    public void showCreateVarDialog() {
        final EditText et = new EditText(this);
        et.setHint("变量名");
        new AlertDialog.Builder(this)
            .setTitle("创建变量")
            .setView(et)
            .setPositiveButton("创建", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    String name = et.getText().toString();
                    if (name.length() > 0) {
                        GameVar gv = new GameVar();
                        gv.varName = name;
                        gv.baseValue = 0;
                        gv.varType = "normal";
                        EngineData.varConfigList.add(gv);
                        EngineData.gameVar.put(name, 0);
                        Toast.makeText(MainActivity.this, "已创建变量: " + name, Toast.LENGTH_SHORT).show();
                        drawCanvas.safeInvalidate();
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    public void showVarManageDialog() {
        if (EngineData.varConfigList.size() == 0) {
            Toast.makeText(this, "暂无变量", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[EngineData.varConfigList.size()];
        for (int i = 0; i < EngineData.varConfigList.size(); i++) {
            GameVar gv = EngineData.varConfigList.get(i);
            names[i] = gv.varName + " (" + gv.varType + ")";
        }
        new AlertDialog.Builder(this)
            .setTitle("变量管理")
            .setItems(names, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    final GameVar gv = EngineData.varConfigList.get(which);
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle(gv.varName)
                        .setItems(new String[]{"设为随机", "设为普通", "删除"}, new android.content.DialogInterface.OnClickListener() {
                            public void onClick(android.content.DialogInterface dialog2, int which2) {
                                if (which2 == 0) { gv.varType = "random"; }
                                else if (which2 == 1) { gv.varType = "normal"; }
                                else if (which2 == 2) {
                                    EngineData.varConfigList.remove(gv);
                                    EngineData.gameVar.remove(gv.varName);
                                }
                                Toast.makeText(MainActivity.this, "已更新", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                }
            })
            .setNegativeButton("关闭", null)
            .show();
    }

    protected void onDestroy() {
        super.onDestroy();
        if (drawCanvas != null) {
            drawCanvas.stopBgm();
            drawCanvas.stopIdleTimer();
        }
    }
}
