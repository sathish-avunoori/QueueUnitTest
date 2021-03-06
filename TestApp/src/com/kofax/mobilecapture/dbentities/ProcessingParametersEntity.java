package com.kofax.mobilecapture.dbentities;

import com.kofax.mobilecapture.dbentities.DaoSession;
import de.greenrobot.dao.DaoException;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table PROCESSING_PARAMETERS_ENTITY.
 */
public class ProcessingParametersEntity {

    private Long processId;
    private String documentTypeName;
    private byte[] serializeDocument;
    private long userInformationId;

    /** Used to resolve relations */
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    private transient ProcessingParametersEntityDao myDao;

    private UserInformationEntity userInformationEntity;
    private Long userInformationEntity__resolvedKey;


    public ProcessingParametersEntity() {
    }

    public ProcessingParametersEntity(Long processId) {
        this.processId = processId;
    }

    public ProcessingParametersEntity(Long processId, String documentTypeName, byte[] serializeDocument, long userInformationId) {
        this.processId = processId;
        this.documentTypeName = documentTypeName;
        this.serializeDocument = serializeDocument;
        this.userInformationId = userInformationId;
    }

    /** called by internal mechanisms, do not call yourself. */
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getProcessingParametersEntityDao() : null;
    }

    public Long getProcessId() {
        return processId;
    }

    public void setProcessId(Long processId) {
        this.processId = processId;
    }

    public String getDocumentTypeName() {
        return documentTypeName;
    }

    public void setDocumentTypeName(String documentTypeName) {
        this.documentTypeName = documentTypeName;
    }

    public byte[] getSerializeDocument() {
        return serializeDocument;
    }

    public void setSerializeDocument(byte[] serializeDocument) {
        this.serializeDocument = serializeDocument;
    }

    public long getUserInformationId() {
        return userInformationId;
    }

    public void setUserInformationId(long userInformationId) {
        this.userInformationId = userInformationId;
    }

    /** To-one relationship, resolved on first access. */
    public UserInformationEntity getUserInformationEntity() {
        long __key = this.userInformationId;
        if (userInformationEntity__resolvedKey == null || !userInformationEntity__resolvedKey.equals(__key)) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            UserInformationEntityDao targetDao = daoSession.getUserInformationEntityDao();
            UserInformationEntity userInformationEntityNew = targetDao.load(__key);
            synchronized (this) {
                userInformationEntity = userInformationEntityNew;
            	userInformationEntity__resolvedKey = __key;
            }
        }
        return userInformationEntity;
    }

    public void setUserInformationEntity(UserInformationEntity userInformationEntity) {
        if (userInformationEntity == null) {
            throw new DaoException("To-one property 'userInformationId' has not-null constraint; cannot set to-one to null");
        }
        synchronized (this) {
            this.userInformationEntity = userInformationEntity;
            userInformationId = userInformationEntity.getUserInformationId();
            userInformationEntity__resolvedKey = userInformationId;
        }
    }

    /** Convenient call for {@link AbstractDao#delete(Object)}. Entity must attached to an entity context. */
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.delete(this);
    }

    /** Convenient call for {@link AbstractDao#update(Object)}. Entity must attached to an entity context. */
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.update(this);
    }

    /** Convenient call for {@link AbstractDao#refresh(Object)}. Entity must attached to an entity context. */
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }    
        myDao.refresh(this);
    }

}
