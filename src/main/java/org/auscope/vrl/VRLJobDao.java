package org.auscope.vrl;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class VRLJobDao {

    private HibernateTemplate hibernateTemplate;

    public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
        this.hibernateTemplate = hibernateTemplate;
    }

    public HibernateTemplate getHibernateTemplate() {
        return hibernateTemplate;
    }

    public List<VRLJob> getJobListByUser(final String user) {
        HibernateCallback callback = new HibernateCallback() {
            public Object doInHibernate(Session session) 
                throws HibernateException, SQLException {
                return session.createQuery(
                        "FROM VRLJob WHERE user='"+user+"'").list();
            }
        };
        return (List<VRLJob>)hibernateTemplate.execute(callback);
    }

    public VRLJob getJobByUserAndRef(final String user,
                                      final String reference) {
        HibernateCallback callback = new HibernateCallback() {
            public Object doInHibernate(Session session) 
                throws HibernateException, SQLException {
                return session.createQuery(
                        "FROM VRLJob WHERE user='"+user+"' AND reference='" +
                        reference + "'").uniqueResult();
            }
        };
        return (VRLJob)hibernateTemplate.execute(callback);
    }

    public void saveVRLJob(final VRLJob vrlJob) {
        HibernateCallback callback = new HibernateCallback() {
            public Object doInHibernate(Session session)
                throws HibernateException, SQLException {
                session.saveOrUpdate(vrlJob);
                return null;
            }
        };
        hibernateTemplate.execute(callback);
    }
}

