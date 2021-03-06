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
package org.n52.shared.requests.query.responses;

import java.io.Serializable;

import org.n52.shared.requests.query.ResultPage;
import org.n52.shared.requests.query.ResultPager;
import org.n52.shared.requests.query.queries.QueryRequest;

/**
 * A response object containing the results of a {@link QueryRequest}. The results can be either paged (a
 * sub-collection of all available results) or a complete collection of all results available.
 */
public abstract class QueryResponse<T> implements Serializable {

    private static final long serialVersionUID = 8964914430932650368L;

    private String serviceUrl;

    private ResultPage<T> results;

    private boolean paged;

    protected QueryResponse() {
        // for serialization
    }

    public QueryResponse(String serviceUrl) {
        this(serviceUrl, null);
    }

    public QueryResponse(String serviceUrl, T[] results) {
        this.serviceUrl = serviceUrl;
        setResults(results);
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }
    
    /**
     * @return the results.
     */
    public T[] getResults() {
        return results.getResults();
    }

    /**
     * Sets the given results as complete collection of all results available.
     * 
     * @param results
     *        the results to set.
     * @see #setResultPage(ResultPage)
     */
    public void setResults(T[] results) {
        int size = results == null ? 0 : results.length;
        this.results = new ResultPager().createPageFrom(results, 0, size);
        this.paged = false;
    }

    /**
     * Sets the given page as a sub-collection of all results available.
     * 
     * @param resultPage
     *        the result page to set.
     * @see #setResults(T[])
     */
    public void setResultPage(ResultPage<T> resultPage) {
        this.results = resultPage;
        this.paged = true;
    }

    /**
     * Returns all query results on a page, no matter if it is a sub-collection or all results available.
     * 
     * @return the results on a page.
     */
    public ResultPage<T> getPagedResults() {
        return results;
    }

    /**
     * @return <code>true</code> the query results are paged, <code>false</code> otherwise.
     */
    public boolean isPaged() {
        return paged;
    }

}
