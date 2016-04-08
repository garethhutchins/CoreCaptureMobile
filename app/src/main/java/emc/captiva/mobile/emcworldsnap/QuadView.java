/** -------------------------------------------------------------------------
 * Copyright 2013-2016 EMC Corporation.  All rights reserved.
 ---------------------------------------------------------------------------- */

package emc.captiva.mobile.emcworldsnap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import emc.captiva.mobile.sdk.CaptureImage;

/**
 * This class draws a quad overlay
 */
public class QuadView extends View {

    private Path _path = new Path();
    private Paint _paint = new Paint();
    private PointF[] _quadPoints;       // The view's quad points
    private boolean _valid;             // true for good quality

    public QuadView(Context context) {
        super(context);

        _paint.setStyle(Paint.Style.STROKE);
        _paint.setAntiAlias(true);
        _paint.setStrokeWidth(5);
    }

    void setQuadCorners(PointF[] quadPoints, boolean valid)
    {
        setVisibility(View.VISIBLE);
        _quadPoints = quadPoints;
        _valid = valid;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (_quadPoints != null) {
            _path.reset();

            // top left
            _path.moveTo(_quadPoints[0].x, _quadPoints[0].y);
            // top right
            _path.lineTo(_quadPoints[1].x, _quadPoints[1].y);
            // bottom right
            _path.lineTo(_quadPoints[2].x, _quadPoints[2].y);
            // bottom left
            _path.lineTo(_quadPoints[3].x, _quadPoints[3].y);
            // back to top left to make it complete
            _path.lineTo(_quadPoints[0].x, _quadPoints[0].y);
            _path.close();

            _paint.setColor(_valid ? Color.GREEN: Color.RED);
            canvas.drawPath(_path, _paint);
        }
    }
}
