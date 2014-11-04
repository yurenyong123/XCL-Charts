/**
 * Copyright 2014  XCL-Charts
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 	
 * @Project XCL-Charts 
 * @Description Android图表基类库
 * @author XiongChuanLiang<br/>(xcl_168@aliyun.com)
 * @license http://www.apache.org/licenses/  Apache v2 License
 * @version 1.0
 * v1.3 2014-8-30 xcl 增加平滑区域面积图
 */
package org.xclcharts.chart;


import java.util.ArrayList;
import java.util.List;

import org.xclcharts.common.CurveHelper;
import org.xclcharts.common.DrawHelper;
import org.xclcharts.common.MathHelper;
import org.xclcharts.common.PointHelper;
import org.xclcharts.renderer.LnChart;
import org.xclcharts.renderer.XEnum;
import org.xclcharts.renderer.line.PlotDot;
import org.xclcharts.renderer.line.PlotDotRender;
import org.xclcharts.renderer.line.PlotLine;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;


/**
 * @ClassName AreaChart
 * @Description  面积图基类
 * @author XiongChuanLiang<br/>(xcl_168@aliyun.com)
 */

public class AreaChart extends LnChart{	
	
	private static final String TAG="AreaChart";
	
	//画点分类的画笔
  	protected Paint mPaintAreaFill =  null; 
  	
    //数据源
  	protected List<AreaData> mDataSet;
  	
  	//透明度
  	private int mAreaAlpha = 100;  	
  	
  	//path area
  	private List<PointF> mLstPathPoints =new ArrayList<PointF>(); 
  	private Path mPathArea = null;
  	private PointF[] mBezierControls = new PointF[2];
	
	//key
  	private List<LnData> mLstKey = new ArrayList<LnData>();	
	
	//line
  	private List<PointF> mLstPoints = new ArrayList<PointF>();	
	
	//dots
  	private List<RectF> mLstDots =new ArrayList<RectF>();
  	
  	//平滑曲线
  	private XEnum.CrurveLineStyle mCrurveLineStyle = XEnum.CrurveLineStyle.BEZIERCURVE;	
  	
  	private final int Y_MIN = 0;
  	private final int Y_MAX = 1;
			  	
	public AreaChart()
	{			
		categoryAxisDefaultSetting();
		dataAxisDefaultSetting();
	}
	
	@Override
	public XEnum.ChartType getType()
	{
		return XEnum.ChartType.AREA;
	}
	
	private void initPaint()
	{
		if(null == mPaintAreaFill)
		{
			mPaintAreaFill = new Paint();
			mPaintAreaFill.setStyle(Style.FILL);
			mPaintAreaFill.setAntiAlias(true);
			mPaintAreaFill.setColor((int)Color.rgb(73, 172, 72));	
		}
	}
	
	
	protected void categoryAxisDefaultSetting()
	{		
		if(null != categoryAxis)categoryAxis.setHorizontalTickAlign(Align.CENTER);
	}
		
	protected void dataAxisDefaultSetting()
	{		
		if(null != dataAxis)dataAxis.setHorizontalTickAlign(Align.LEFT);
	}		
				
	 /**
	 * 分类轴的数据源
	 * @param categories 分类集
	 */
	public void setCategories(List<String> categories)
	{				
		if(null != categoryAxis)categoryAxis.setDataBuilding(categories);
	}
	
	/**
	 *  设置数据轴的数据源
	 * @param dataset 数据源
	 */
	public void setDataSource(List<AreaData> dataset)
	{				
		this.mDataSet = dataset;		
	}
	
	/**
	 * 设置透明度,默认为100
	 * @param alpha 透明度
	 */
	public void setAreaAlpha(int alpha)
	{
		mAreaAlpha = alpha;
	}	
	
	
	/**
	 * 设置曲线显示风格:直线(NORMAL)或平滑曲线(BEZIERCURVE)
	 * @param style
	 */
	public void setCrurveLineStyle(XEnum.CrurveLineStyle style)
	{
		mCrurveLineStyle = style;
	}
	
	/**
	 * 返回曲线显示风格
	 * @return 显示风格
	 */
	public XEnum.CrurveLineStyle getCrurveLineStyle()
	{
		return mCrurveLineStyle;
	}
	
	private boolean calcAllPoints(AreaData bd,
			List<RectF> lstDots,
			List<PointF> lstPoints,
			List<PointF> lstPathPoints)
	{
		
		if(null == bd)
		{
			Log.e(TAG,"传入的数据序列参数为空.");
			return false;
		}
		//数据源
		List<Double> chartValues = bd.getLinePoint();
		if(null == chartValues)
		{
			Log.e(TAG,"线数据集合为空.");
			return false;
		}				
				
		float initX =  plotArea.getLeft();
        float initY =  plotArea.getBottom();
         
		float lineStartX = initX,lineStartY = initY;
        float lineStopX = 0.0f,lineStopY = 0.0f;     
        						
		float axisScreenHeight = getPlotScreenHeight();
		float axisDataHeight =  (float) dataAxis.getAxisRange();	
		float currLablesSteps = div(getPlotScreenWidth(), (categoryAxis.getDataSet().size() -1));
		  
        double dper = 0d;
		int j = 0;	 
		int count = chartValues.size();
		if(count <= 0) return false;
								
		for(Double bv : chartValues)
        {								
			//参数值与最大值的比例  照搬到 y轴高度与矩形高度的比例上来 	                                
        	//float valuePosition = (float) Math.round(
			//		axisScreenHeight * ( (bv - dataAxis.getAxisMin() ) / axisDataHeight)) ;    
		
			//首尾为0,path不能闭合，改成 0.001都可以闭合?
        	dper = MathHelper.getInstance().sub(bv, dataAxis.getAxisMin()); 
        	float valuePosition = mul(axisScreenHeight, div(dtof(dper),axisDataHeight) );
        	        	
        	lineStopX = add(initX , j * currLablesSteps);        	
        	lineStopY = sub(initY , valuePosition);  
        	        	
        	if(0 == j)
        	{
        		lineStartX = lineStopX;
        		lineStartY = lineStopY;
        				        		
        		if(2 < count)
        		{
        			if(Double.compare( bv, dataAxis.getAxisMin() ) != 0  ) 
        								lstPathPoints.add( new PointF(initX,initY));
        		}
        			
        		lstPathPoints.add( new PointF(lineStartX,lineStartY));
        		lstPoints.add( new PointF(lineStartX,lineStartY));
        	}
        	
        	//path
        	lstPathPoints.add( new PointF(lineStopX,lineStopY));    
        	        	        	        
        	//line
    		lstPoints.add( new PointF(lineStopX,lineStopY));        	
        	
        	//dot
        	lstDots.add(new RectF(lineStartX,lineStartY,lineStopX,lineStopY));
   	
        	lineStartX = lineStopX;
			lineStartY = lineStopY;
        	
        	j++;
        }
		
		if(count > 2 )
		{
			lstPathPoints.add( new PointF(lineStartX ,lineStartY));
			
			if(Double.compare( chartValues.get(count - 1), dataAxis.getAxisMin() ) != 0  )
										lstPathPoints.add( new PointF(lineStartX ,initY));
		}
		return true;        
	}
	
		
	private boolean renderBezierArea(Canvas canvas, Paint paintAreaFill,
			Path bezierPath,
			AreaData areaData,
			List<PointF> lstPathPoints)
	{	
	int count = lstPathPoints.size();		
	if(count < 3) return false; //没有或仅一个点就不需要了
			
				
	//设置当前填充色
	paintAreaFill.setColor(areaData.getAreaFillColor());			

	//仅两点
	if(count == 3)
	{						
		if(null == bezierPath)bezierPath = new Path();
	
		bezierPath.moveTo(lstPathPoints.get(0).x, plotArea.getBottom());
		bezierPath.lineTo(lstPathPoints.get(0).x, lstPathPoints.get(0).y);
		
		PointF ctl3 = PointHelper.percent(lstPathPoints.get(1),0.5f, lstPathPoints.get(2),0.8f) ;
		bezierPath.quadTo(ctl3.x, ctl3.y, lstPathPoints.get(2).x, lstPathPoints.get(2).y);		
		
		//canvas.drawCircle(ctl3.x, ctl3.y, 10.f, paintAreaFill);		//显示控制点
		
		bezierPath.lineTo(lstPathPoints.get(2).x, plotArea.getBottom());
		bezierPath.close();
		
		
		if(areaData.getApplayGradient())
		{				
			
			LinearGradient  linearGradient ;
			if(areaData.getGradientDirection() == XEnum.Direction.VERTICAL)
			{
				float lineMaxY = getLineMaxMinY(Y_MAX,lstPathPoints);
				linearGradient = new LinearGradient(
						0, 0, 0,lineMaxY,// 2 * h,
						// plotArea.getLeft(),plotArea.getBottom(), lstPathPoints.get(count -1).x,lineMinY,
						 areaData.getAreaBeginColor(),areaData.getAreaEndColor(), 				 
						 areaData.getGradientMode());
			}else{
				float lineMinY = getLineMaxMinY(Y_MIN,lstPathPoints);
				linearGradient = new LinearGradient(
						 plotArea.getLeft(),plotArea.getBottom(), lstPathPoints.get(2).x,lineMinY,
						 areaData.getAreaBeginColor(),areaData.getAreaEndColor(), 				 
						 areaData.getGradientMode());
			}
			
			/*
			float lineMinY = getLineMinY(lstPathPoints);
			
			//float h = plotArea.getBottom() - lineMinY; 				
			//if( areaData.getGradientMode() == Shader.TileMode.MIRROR) h = 2*h;
			
			LinearGradient  linearGradient = new LinearGradient(
					//0, 0, 0, h,//2 * h,
					plotArea.getLeft(), plotArea.getBottom(),lstPathPoints.get(2).x,lineMinY,
					 areaData.getAreaBeginColor(),areaData.getAreaEndColor(), 				 
					 areaData.getGradientMode());
					//   areaData.getGradientColors(), null,areaData.getGradientMode());  	
			*/
			paintAreaFill.setShader(linearGradient);   
			
		}else{
			paintAreaFill.setShader(null);
		}
		
		
	    canvas.drawPath(bezierPath, paintAreaFill);		
		bezierPath.reset();		
		return true;
	}

	//start point
	bezierPath.moveTo(plotArea.getLeft(), plotArea.getBottom()); 
	
	for(int i = 0;i<count;i++)
	{					
		if(i<3)continue; 
										
		CurveHelper.curve3( lstPathPoints.get(i-2),  
				lstPathPoints.get(i-1), 
				lstPathPoints.get(i-3),
				lstPathPoints.get(i), 
				mBezierControls);
		
		bezierPath.cubicTo( mBezierControls[0].x, mBezierControls[0].y, 
				mBezierControls[1].x, mBezierControls[1].y, 
				lstPathPoints.get(i -1 ).x, lstPathPoints.get(i -1 ).y);		
	}			
	
	
	PointF stop = lstPathPoints.get(count -1);	
	if( Float.compare(stop.y, plotArea.getBottom()) == 0 ) 
	{		
		bezierPath.lineTo(stop.x, stop.y);	
	}else{	
		//PointF start = lstPathPoints.get(lstPathPoints.size()-2);						
		CurveHelper.curve3(lstPathPoints.get(count -2),  
									stop, 
									lstPathPoints.get(count - 3),
									stop, 
									mBezierControls);
		bezierPath.cubicTo( mBezierControls[0].x, mBezierControls[0].y, 
							mBezierControls[1].x, mBezierControls[1].y, 
							stop.x, 
							stop.y);		
	}	

	bezierPath.close();
	
	
	//透明度
	paintAreaFill.setAlpha(this.mAreaAlpha); 
	
	if(areaData.getApplayGradient())
	{			
		
		LinearGradient  linearGradient ;
		if(areaData.getGradientDirection() == XEnum.Direction.VERTICAL)
		{
			float lineMaxY = getLineMaxMinY(Y_MAX,lstPathPoints);
			linearGradient = new LinearGradient(
					0, 0, 0,lineMaxY,// 2 * h,
					// plotArea.getLeft(),plotArea.getBottom(), lstPathPoints.get(count -1).x,lineMinY,
					 areaData.getAreaBeginColor(),areaData.getAreaEndColor(), 				 
					 areaData.getGradientMode());
		}else{
			float lineMinY = getLineMaxMinY(Y_MIN,lstPathPoints);
			linearGradient = new LinearGradient(
					 plotArea.getLeft(), plotArea.getBottom(),stop.x,lineMinY,
					 areaData.getAreaBeginColor(),areaData.getAreaEndColor(), 				 
					 areaData.getGradientMode());
		}
		
		/*
		float lineMinY = getLineMinY(lstPathPoints);
		
		//float h = plotArea.getBottom() - lineMinY; 				
		//if( areaData.getGradientMode() == Shader.TileMode.MIRROR) h = 2*h;
		
		LinearGradient  linearGradient = new LinearGradient(
				//0, 0, 0, h,//2 * h,
				plotArea.getLeft(), plotArea.getBottom(),stop.x,lineMinY,
				 areaData.getAreaBeginColor(),areaData.getAreaEndColor(), 				 
				 areaData.getGradientMode());
				//   areaData.getGradientColors(), null,areaData.getGradientMode());  	
		*/
		paintAreaFill.setShader(linearGradient);   
		
	}else{
		paintAreaFill.setShader(null);
	}
	
		
    canvas.drawPath(bezierPath, paintAreaFill);	
    bezierPath.reset();
       
	return true;
}

	
	private boolean renderArea(Canvas canvas,Paint paintAreaFill,Path pathArea,
								AreaData areaData,
								List<PointF> lstPathPoints)
	{		        		
		int count = lstPathPoints.size();
		if(count <= 0) return false;
		
		for(int i = 0;i<count;i++)
		{
			PointF point = lstPathPoints.get(i);
        	if(0 == i)
        	{
        		pathArea.moveTo(point.x ,point.y);  
        	}else{
        		pathArea.lineTo(point.x ,point.y);   
        	}				
		}							
		pathArea.close();
			
					
		//设置当前填充色
		paintAreaFill.setColor(areaData.getAreaFillColor());		
		
		if(areaData.getApplayGradient())
		{			
			//float h = plotArea.getBottom() - lineMinY; 		
			//if( areaData.getGradientMode() == Shader.TileMode.MIRROR) h = 2*h;
			
			LinearGradient  linearGradient ;
			if(areaData.getGradientDirection() == XEnum.Direction.VERTICAL)
			{
				float lineMaxY = getLineMaxMinY(Y_MAX,lstPathPoints);
				linearGradient = new LinearGradient(
						0, 0, 0,lineMaxY,// 2 * h,
						// plotArea.getLeft(),plotArea.getBottom(), lstPathPoints.get(count -1).x,lineMinY,
						 areaData.getAreaBeginColor(),areaData.getAreaEndColor(), 				 
						 areaData.getGradientMode());
			}else{
				float lineMinY = getLineMaxMinY(Y_MIN,lstPathPoints);
				linearGradient = new LinearGradient(
						 plotArea.getLeft(),plotArea.getBottom(), lstPathPoints.get(count -1).x,lineMinY,
						 areaData.getAreaBeginColor(),areaData.getAreaEndColor(), 				 
						 areaData.getGradientMode());
			}
			
			/*
			LinearGradient  linearGradient = new LinearGradient(
					0, 0, 0,lineMinY,// 2 * h,
					// plotArea.getLeft(),plotArea.getBottom(), lstPathPoints.get(count -1).x,lineMinY,
					 areaData.getAreaBeginColor(),areaData.getAreaEndColor(), 				 
					 areaData.getGradientMode());
					//   areaData.getGradientColors(), null,areaData.getGradientMode());  	
			*/
			
			paintAreaFill.setShader(linearGradient);    
		}else{
			paintAreaFill.setShader(null);
		}
		//透明度
		paintAreaFill.setAlpha(this.mAreaAlpha); 
				
		//绘制area
	    canvas.drawPath(pathArea, paintAreaFill); //paintAreaFill);	      
	    pathArea.reset();		
		return true;
	}
				
	private boolean renderLine(Canvas canvas, AreaData areaData,
								List<PointF> lstPoints)
	{		  
		int count = lstPoints.size();
		for(int i=0;i<count;i++)
        {	        	
        	if(0 == i)continue;
        	PointF pointStart = lstPoints.get(i - 1);
        	PointF pointStop = lstPoints.get(i);
        	            	
  	      	DrawHelper.getInstance().drawLine(areaData.getLineStyle(), 
  	    		  		pointStart.x ,pointStart.y ,pointStop.x ,pointStop.y,
  	    		  		canvas,areaData.getLinePaint());	
        }
		
		return true;
	}
	
	
	private boolean renderBezierCurveLine(Canvas canvas,Path bezierPath,
											AreaData areaData,List<PointF> lstPoints)
	{		        		
		renderBezierCurveLine(canvas,areaData.getLinePaint(),bezierPath,lstPoints); 		 
		return true;
	}

	/**
	 * 绘制区域
	 * @param bd	数据序列
	 * @param type	绘制类型
	 * @param alpha 透明度
	 */
	private boolean renderDotAndLabel(Canvas canvas, AreaData bd,int dataID,
										List<RectF> lstDots)
	{
		
		 PlotLine pLine = bd.getPlotLine();
		 if(pLine.getDotStyle().equals(XEnum.DotStyle.HIDE) == true 
				 					&&bd.getLabelVisible() == false )
		 {
    	   return true;
		 }
		 int childID = 0;

		//数据源
		List<Double> chartValues = bd.getLinePoint();
		if(null == chartValues)
		{
			Log.e(TAG,"线数据集合为空.");
			return false;
		}			
		float itemAngle = bd.getItemLabelRotateAngle();
		int count = lstDots.size();
		for(int i=0;i<count;i++)
		{
			Double dv = chartValues.get(i);			
			RectF  dot = lstDots.get(i);
			
			float radius = 0.0f;
			if(!pLine.getDotStyle().equals(XEnum.DotStyle.HIDE))
        	{            	
        		PlotDot pDot = pLine.getPlotDot();	
        		radius = pDot.getDotRadius();
        		float rendEndX  = add(dot.right  , radius);    
        		            	      
        		PlotDotRender.getInstance().renderDot(canvas,pDot,
        				dot.left ,dot.top ,
        				dot.right ,dot.bottom,
        				pLine.getDotPaint()); //标识图形            			                	
        		dot.right = rendEndX;
    			
    			savePointRecord(dataID,childID, dot.right - radius+ mMoveX, dot.bottom + mMoveY,
    					dot.right  - 2*radius + mMoveX, dot.bottom - radius + mMoveY,
    					dot.right  + mMoveX			  , dot.bottom + radius + mMoveY);
    			
    			childID++;
        	}
    		
    		if(bd.getLabelVisible())
        	{  
        		bd.getPlotLabel().drawLabel(canvas, pLine.getDotLabelPaint(),
        						getFormatterItemLabel(dv),dot.right - radius ,dot.bottom,itemAngle,bd.getLineColor());        		
        	} 
		}
		return true;
	}	
						
	
	private float getLineMaxMinY(int type,List<PointF> lstPathPoints)
	{		
		//渲染高度
		float lineMaxY = 0.0f;
		
		float lineMinY = 0.0f;	
		int count = lstPathPoints.size();
		
		for(int i = 0;i<count;i++)
		{				
			if(Y_MAX == type)
			{
				if( lineMaxY < lstPathPoints.get(i).y )
						lineMaxY = lstPathPoints.get(i).y;
			}else if(Y_MIN == type){
				if(0 == i)
				{
					lineMinY = lstPathPoints.get(0).y;
				}else{
					if( lineMinY > lstPathPoints.get(i).y )
						lineMinY = lstPathPoints.get(i).y;
				}
			}
		}		
		if(Y_MAX == type)
		{
			return lineMaxY;
		}else { // if(Y_MIN == type){
			return lineMinY;
		}
	}
	
	private boolean renderVerticalPlot(Canvas canvas)
	{								
		if(null == mDataSet)
		{
			Log.e(TAG,"数据源为空.");
			return false;
		}
		
		this.initPaint();
		if(null == mPathArea) mPathArea = new Path();
								
		//透明度。其取值范围是0---255,数值越小，越透明，颜色上表现越淡             
		//mPaintAreaFill.setAlpha( mAreaAlpha );  		
		
						
		//开始处 X 轴 即分类轴                  
		int count = mDataSet.size();
		for(int i=0;i<count;i++)
		{					
			AreaData areaData = mDataSet.get(i);
			
			calcAllPoints( areaData,mLstDots,mLstPoints,mLstPathPoints);	
					
			switch(getCrurveLineStyle())
			{
				case BEZIERCURVE:
					renderBezierArea(canvas,mPaintAreaFill,mPathArea,areaData,mLstPathPoints);
					renderBezierCurveLine(canvas,mPathArea,areaData,mLstPoints);
					break;
				case BEELINE:
					renderArea(canvas,mPaintAreaFill,mPathArea,areaData,mLstPathPoints);	
					renderLine(canvas,areaData,mLstPoints);	
					break;
				default:
					Log.e(TAG,"未知的枚举类型.");
					continue;				
			}								
			renderDotAndLabel(canvas,areaData,i,mLstDots);						
			mLstKey.add(areaData);
			
			mLstDots.clear();
			mLstPoints.clear();
			mLstPathPoints.clear();						
		}					
		return true;
	}


	/////////////////////////////////////////////	
	@Override
	protected void drawClipPlot(Canvas canvas)
	{
		if(renderVerticalPlot(canvas) == true)
		{				
			//画横向定制线
			////mCustomLine.setVerticalPlot(dataAxis, plotArea, getAxisScreenHeight());
			////ret = mCustomLine.renderVerticalCustomlinesDataAxis(canvas);													
		}
	}

	@Override
	protected void drawClipLegend(Canvas canvas)
	{
		plotLegend.renderLineKey(canvas, mLstKey);
		mLstKey.clear();
	}
	/////////////////////////////////////////////

	 
}
