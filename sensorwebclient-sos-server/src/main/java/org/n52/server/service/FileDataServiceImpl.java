/**
 * ﻿Copyright (C) 2012-2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
package org.n52.server.service;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.n52.oxf.sos.adapter.ISOSRequestBuilder.DESCRIBE_SENSOR_OUTPUT_FORMAT;
import static org.n52.oxf.sos.adapter.ISOSRequestBuilder.DESCRIBE_SENSOR_PROCEDURE_DESCRIPTION_FORMAT;
import static org.n52.oxf.sos.adapter.ISOSRequestBuilder.DESCRIBE_SENSOR_PROCEDURE_PARAMETER;
import static org.n52.oxf.sos.adapter.ISOSRequestBuilder.DESCRIBE_SENSOR_SERVICE_PARAMETER;
import static org.n52.oxf.sos.adapter.ISOSRequestBuilder.DESCRIBE_SENSOR_VERSION_PARAMETER;
import static org.n52.oxf.sos.adapter.SOSAdapter.DESCRIBE_SENSOR;
import static org.n52.server.mgmt.ConfigurationContext.SERVER_TIMEOUT;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.FutureTask;

import org.n52.client.service.FileDataService;
import org.n52.oxf.adapter.OperationResult;
import org.n52.oxf.adapter.ParameterContainer;
import org.n52.oxf.ows.capabilities.Operation;
import org.n52.oxf.sos.adapter.ISOSRequestBuilder;
import org.n52.oxf.sos.util.SosUtil;
import org.n52.oxf.util.IOHelper;
import org.n52.oxf.util.JavaHelper;
import org.n52.server.da.AccessorThreadPool;
import org.n52.server.da.oxf.OperationAccessor;
import org.n52.server.da.oxf.SOSAdapter_OXFExtension;
import org.n52.server.da.oxf.SosRequestBuilderFactory;
import org.n52.server.io.CsvGenerator;
import org.n52.server.io.Generator;
import org.n52.server.io.PdfGenerator;
import org.n52.server.io.XlsGenerator;
import org.n52.server.io.ZipGenerator;
import org.n52.server.mgmt.ConfigurationContext;
import org.n52.shared.exceptions.ServerException;
import org.n52.shared.exceptions.TimeoutException;
import org.n52.shared.requests.TimeSeriesDataRequest;
import org.n52.shared.responses.RepresentationResponse;
import org.n52.shared.serializable.pojos.DesignOptions;
import org.n52.shared.serializable.pojos.TimeseriesProperties;
import org.n52.shared.serializable.pojos.sos.SOSMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDataServiceImpl implements FileDataService {
    
    private static final Logger LOG = LoggerFactory.getLogger(FileDataServiceImpl.class);

    @Override
    public RepresentationResponse getPDF(TimeSeriesDataRequest req) throws Exception {
        try {
            PdfGenerator gen = new PdfGenerator(false, "");
            return gen.producePresentation(req.getOptions());
        } catch (Exception e) {
            LOG.error("Exception occured on server side.", e);
            throw e; // last chance to log on server side
        }
    }

    @Override
    public RepresentationResponse getXLS(TimeSeriesDataRequest req) throws Exception {
        try {
            XlsGenerator gen = new XlsGenerator(false, "");
            return gen.producePresentation(req.getOptions());
        } catch (Exception e) {
            LOG.error("Exception occured on server side.", e);
            throw e; // last chance to log on server side
        }
    }

    @Override
    public RepresentationResponse getCSV(TimeSeriesDataRequest req) throws Exception {
        try {
            CsvGenerator gen = new CsvGenerator(false, "");
            return gen.producePresentation(req.getOptions());
        } catch (Exception e) {
            LOG.error("Exception occured on server side.", e);
            throw e; // last chance to log on server side
        }
    }

    @Override
    public RepresentationResponse getPDFzip(TimeSeriesDataRequest req) throws Exception {
        try {
            Date d = new Date();
            String folder = "zip"+d.getTime()+"/";
            PdfGenerator gen = new PdfGenerator(true, folder);
            // generate all timeseries on their own
            return generateZipPresentation(req, gen);
        }  catch (Exception e) {
            LOG.error("Exception occured on server side.", e);
            throw e; // last chance to log on server side
        }
    }

    @Override
    public RepresentationResponse getXLSzip(TimeSeriesDataRequest req) throws Exception {
        try {
            Date d = new Date();
            String folder = "zip" + d.getTime();
            XlsGenerator gen = new XlsGenerator(true, folder);
            // generate all timeseries on their own
            return generateZipPresentation(req, gen);
        } catch (Exception e) {
            LOG.error("Exception occured on server side.", e);
            throw e; // last chance to log on server side
        }
    }

    @Override
    public RepresentationResponse getCSVzip(TimeSeriesDataRequest req) throws Exception {
        try {
            Date d = new Date();
            String folder = "zip"+d.getTime();
            CsvGenerator gen = new CsvGenerator(true, folder);
            // generate all timeseries on their own
            return generateZipPresentation(req, gen);
        } catch (Exception e) {
            LOG.error("Exception occured on server side.", e);
            throw e; // last chance to log on server side
        }
    }

    private RepresentationResponse generateZipPresentation(TimeSeriesDataRequest req, Generator gen) throws ServerException {
        try {
            for (TimeseriesProperties prop : req.getOptions().getProperties()) {
                generateSensorMLFile(sendDescSens(prop), prop, gen.getFolderPostfix());
                ArrayList<TimeseriesProperties> props = new ArrayList<TimeseriesProperties>();
                props.add(prop);
                gen.producePresentation(new DesignOptions(props, req.getOptions()
                        .getBegin(), req.getOptions().getEnd(), req.getOptions().getGrid()));
            }
            // so we got our files in the right folder, now zippem
            ZipGenerator zipgen = new ZipGenerator(gen.getFolderPostfix());
            return zipgen.producePresentation(req.getOptions());
        } catch (Exception e) {
            // wrap generic exception
            throw new ServerException("Could not generate ZIP file.", e);
        }
    }
    

    private void generateSensorMLFile(OperationResult sendDescSens, TimeseriesProperties prop, String folderPostfix) throws ServerException {
        try {
            File f = JavaHelper.genFile(ConfigurationContext.GEN_DIR+"/"+folderPostfix, 
                    "SensorML_" + prop.getProcedure().replaceAll("/", "_"), "xml");
            IOHelper.saveFile(f, sendDescSens.getIncomingResultAsStream());
        } catch (Exception e) {
            // wrap generic exception
            throw new ServerException("Could not save file.", e);
        }
    }

    private OperationResult sendDescSens(TimeseriesProperties prop) throws ServerException {
        try {
            String sosUrl = prop.getServiceUrl();
            SOSMetadata meta = ConfigurationContext.getSOSMetadata(sosUrl);
            
            OperationResult opResult = null;
            // build up parameterContainer for DescribeSensor operation:
            ParameterContainer paramCon = new ParameterContainer();
    
            String sosVersion = meta.getSosVersion();
            String procedureId = prop.getProcedure();
            String smlVersion = meta.getSensorMLVersion();
            paramCon.addParameterShell(DESCRIBE_SENSOR_SERVICE_PARAMETER, "SOS");
            paramCon.addParameterShell(DESCRIBE_SENSOR_VERSION_PARAMETER, sosVersion);
            paramCon.addParameterShell(DESCRIBE_SENSOR_PROCEDURE_PARAMETER, procedureId);
            if (SosUtil.isVersion100(sosVersion)) {
                paramCon.addParameterShell(DESCRIBE_SENSOR_OUTPUT_FORMAT, smlVersion);
            } else if (SosUtil.isVersion200(sosVersion)) {
                paramCon.addParameterShell(DESCRIBE_SENSOR_PROCEDURE_DESCRIPTION_FORMAT, smlVersion);
            } else {
                throw new IllegalStateException("SOS Version (" + sosVersion + ") is not supported!");
            }
    
            ISOSRequestBuilder requestBuilder = SosRequestBuilderFactory.createRequestBuilder(sosVersion);
            SOSAdapter_OXFExtension adapter = new SOSAdapter_OXFExtension(sosVersion, requestBuilder);
            Operation descSensorOperation = new Operation(DESCRIBE_SENSOR, sosUrl, sosUrl);
            OperationAccessor callable = new OperationAccessor(adapter, descSensorOperation, paramCon);
            FutureTask<OperationResult> t = new FutureTask<OperationResult>(callable);
            try {
                AccessorThreadPool.execute(t);
                opResult = t.get(SERVER_TIMEOUT, MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                throw new TimeoutException("Server did not respond in time", e);
            }
            return opResult;
        } catch (Exception e) {
            // wrap generic exception
            throw new ServerException("Could not get sensor descrioption.", e);
        }
    }

}
