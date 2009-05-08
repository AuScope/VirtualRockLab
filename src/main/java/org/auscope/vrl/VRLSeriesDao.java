package org.auscope.vrl;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class VRLSeriesDao extends HibernateDaoSupport {

    protected final Log logger = LogFactory.getLog(getClass());

    public List<VRLSeries> query(final String user, final String name,
                                 final String desc) {
        String queryString = new String("from VRLSeries s where");
        boolean first = true;
        if (user != null) {
            queryString += " s.user like '%"+user+"%'";
            first = false;
        }

        if (name != null) {
            if (!first) {
                queryString += " and";
            }

            queryString += " s.name like '%"+name+"%'";
            first = false;
        }

        if (desc != null) {
            if (!first) {
                queryString += " and";
            }

            queryString += " s.description like '%"+desc+"%'";
            first = false;
        }

        return (List<VRLSeries>) getHibernateTemplate().find(queryString);
    }

    public VRLSeries get(final int id) {
        return (VRLSeries) getHibernateTemplate().get(VRLSeries.class, id);
    }

    public void save(final VRLSeries series) {
        getHibernateTemplate().saveOrUpdate(series);
    }
}

