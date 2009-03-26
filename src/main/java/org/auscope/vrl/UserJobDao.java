package org.auscope.vrl;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class UserJobDao {

    private HibernateTemplate hibernateTemplate;

    public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
        this.hibernateTemplate = hibernateTemplate;
    }

    public HibernateTemplate getHibernateTemplate() {
        return hibernateTemplate;
    }

    public List<UserJob> getUserJobList(final String userId) {
        HibernateCallback callback = new HibernateCallback() {
            public Object doInHibernate(Session session) 
                throws HibernateException, SQLException {
                return session.createQuery(
                        "FROM UserJob WHERE userId='"+userId+"'").list();
            }
        };
        return (List<UserJob>)hibernateTemplate.execute(callback);
    }

    public UserJob getUserJobByRef(final String userId, final String reference) {
        HibernateCallback callback = new HibernateCallback() {
            public Object doInHibernate(Session session) 
                throws HibernateException, SQLException {
                return session.createQuery(
                        "FROM UserJob WHERE reference='"+reference+"'") .
                    uniqueResult();
            }
        };
        return (UserJob)hibernateTemplate.execute(callback);
    }

    public void saveUserJob(final UserJob userJob) {
        HibernateCallback callback = new HibernateCallback() {
            public Object doInHibernate(Session session)
                throws HibernateException, SQLException {
                session.saveOrUpdate(userJob);
                return null;
            }
        };
        hibernateTemplate.execute(callback);
    }
}

