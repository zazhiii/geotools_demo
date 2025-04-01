package com.zazhi.tutorial.coverage_processor;

import java.io.File;
import java.io.IOException;

import lombok.Data;
//import org.geotools.api.geometry.Bounds;
//import org.geotools.api.parameter.ParameterValueGroup;
//import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.Arguments;
import org.geotools.util.factory.Hints;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * 基于地理范围均分实现栅格数据简单分块。使用栅格处理操作。
 */
@Data
public class ImageTiler {

    private final int NUM_HORIZONTAL_TILES = 16;
    private final int NUM_VERTICAL_TILES = 8;

    private Integer numberOfHorizontalTiles = NUM_HORIZONTAL_TILES;
    private Integer numberOfVerticalTiles = NUM_VERTICAL_TILES;
    private Double tileScale;
    private File inputFile;
    private File outputDirectory;
    private String getFileExtension(File file) throws Exception {
        String name = file.getName();
        return name.substring(name.lastIndexOf(".") + 1);
    }

    public static void main(String[] args) throws Exception {
        Arguments processedArgs = new Arguments(args);
        ImageTiler tiler = new ImageTiler();

        try {
            tiler.setInputFile(new File(processedArgs.getRequiredString("-f")));
            tiler.setOutputDirectory(new File(processedArgs.getRequiredString("-o")));
            tiler.setNumberOfHorizontalTiles(processedArgs.getOptionalInteger("-htc"));
            tiler.setNumberOfVerticalTiles(processedArgs.getOptionalInteger("-vtc"));
            tiler.setTileScale(processedArgs.getOptionalDouble("-scale"));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            printUsage();
            System.exit(1);
        }

        tiler.tile();
    }

    private static void printUsage() {
        System.out.println("用法: -f 输入文件 -o 输出目录 [-tw 瓦片宽度<默认:256> -th 瓦片高度<默认:256>]");
        System.out.println("-htc 水平瓦片数<默认:16> -vtc 垂直瓦片数<默认:8>");
    }

    /**
     * 分块处理栅格数据
     */
    private void tile() throws Exception {
        AbstractGridFormat format = GridFormatFinder.findFormat(this.getInputFile());
        String fileExtension = this.getFileExtension(this.getInputFile());

        // 修复GeoTiff加载时的坐标轴顺序问题
        Hints hints = null;
        if (format instanceof GeoTiffFormat) {
            hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        }

        GridCoverage2DReader gridReader = format.getReader(this.getInputFile(), hints);
        GridCoverage2D gridCoverage = gridReader.read(null);
//        ReferencedEnvelope coverageEnvelope = gridCoverage.getEnvelope2D();
        Envelope2D coverageEnvelope = gridCoverage.getEnvelope2D();
        double coverageMinX = coverageEnvelope.getMinX();
        double coverageMaxX = coverageEnvelope.getMaxX();
        double coverageMinY = coverageEnvelope.getMinY();
        double coverageMaxY = coverageEnvelope.getMaxY();

        // 计算瓦片数
        int htc = this.getNumberOfHorizontalTiles() != null ? this.getNumberOfHorizontalTiles() : NUM_HORIZONTAL_TILES;
        int vtc = this.getNumberOfVerticalTiles() != null ? this.getNumberOfVerticalTiles() : NUM_VERTICAL_TILES;

        // 计算每个瓦片的地理范围
        double geographicTileWidth = (coverageMaxX - coverageMinX) / (double) htc;
        double geographicTileHeight = (coverageMaxY - coverageMinY) / (double) vtc;

        // 获取目标CRS
        CoordinateReferenceSystem targetCRS = gridCoverage.getCoordinateReferenceSystem();

        // 确保输出目录存在
        File tileDirectory = this.getOutputDirectory();
        if (!tileDirectory.exists()) {
            tileDirectory.mkdirs();
        }

        // 循环处理分块
        for (int i = 0; i < htc; i++) {
            for (int j = 0; j < vtc; j++) {
                System.out.println("处理索引 i: " + i + ", j: " + j + " 处的瓦片");
                ReferencedEnvelope envelope = getTileEnvelope(coverageMinX, coverageMinY, geographicTileWidth, geographicTileHeight, targetCRS, i, j);
                GridCoverage2D finalCoverage = cropCoverage(gridCoverage, envelope);

                if (this.getTileScale() != null) {
                    finalCoverage = scaleCoverage(finalCoverage);
                }

                File tileFile = new File(tileDirectory, i + "_" + j + "." + fileExtension);
                format.getWriter(tileFile).write(finalCoverage, null);
            }
        }
    }
//    private Bounds getTileEnvelope(
    private ReferencedEnvelope getTileEnvelope(
            double coverageMinX,
            double coverageMinY,
            double geographicTileWidth,
            double geographicTileHeight,
            CoordinateReferenceSystem targetCRS,
            int horizontalIndex,
            int verticalIndex) {

        double envelopeStartX = (horizontalIndex * geographicTileWidth) + coverageMinX;
        double envelopeEndX = envelopeStartX + geographicTileWidth;
        double envelopeStartY = (verticalIndex * geographicTileHeight) + coverageMinY;
        double envelopeEndY = envelopeStartY + geographicTileHeight;

        return new ReferencedEnvelope(envelopeStartX, envelopeEndX, envelopeStartY, envelopeEndY, targetCRS);
    }

    /**
     * 裁剪栅格数据
     */
    private GridCoverage2D cropCoverage(GridCoverage2D gridCoverage, ReferencedEnvelope envelope) {
        CoverageProcessor processor = CoverageProcessor.getInstance();
        final ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
        param.parameter("Source").setValue(gridCoverage);
        param.parameter("Envelope").setValue(envelope);
        return (GridCoverage2D) processor.doOperation(param);
    }
    /**
     * 按设定比例缩放栅格数据
     *
     * <p>相较于参数操作，Operations类提供更类型安全的方式
     */
    private GridCoverage2D scaleCoverage(GridCoverage2D coverage) {
        Operations ops = new Operations(null);
        coverage = (GridCoverage2D) ops.scale(coverage, this.getTileScale(), this.getTileScale(), 0, 0);
        return coverage;
    }

}