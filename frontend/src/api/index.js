import request from '@/utils/request'

export const zoneApi = {
  getTree() {
    return request.get('/zone/tree')
  },
  getAll() {
    return request.get('/zone/list')
  },
  getById(id) {
    return request.get(`/zone/${id}`)
  },
  create(data) {
    return request.post('/zone', data)
  },
  update(data) {
    return request.put('/zone', data)
  },
  delete(id) {
    return request.delete(`/zone/${id}`)
  }
}

export const lightGroupApi = {
  getPage(params) {
    return request.get('/light-group/page', { params })
  },
  getById(id) {
    return request.get(`/light-group/${id}`)
  },
  getByZoneId(zoneId) {
    return request.get(`/light-group/by-zone/${zoneId}`)
  },
  create(data) {
    return request.post('/light-group', data)
  },
  update(data) {
    return request.put('/light-group', data)
  },
  delete(id) {
    return request.delete(`/light-group/${id}`)
  },
  exportByZoneId(zoneId) {
    return request.get(`/light-group/export/${zoneId}`, { responseType: 'blob' })
  },
  exportSelected(ids) {
    return request.get('/light-group/export-selected', { 
      params: { ids: ids.join(',') },
      responseType: 'blob' 
    })
  }
}

export const transferApi = {
  batchTransfer(data) {
    return request.post('/transfer/batch', data)
  },
  getLedgerPage(params) {
    return request.get('/transfer/ledger/page', { params })
  },
  getLedgerById(id) {
    return request.get(`/transfer/ledger/${id}`)
  },
  getLedgerDetails(id) {
    return request.get(`/transfer/ledger/${id}/details`)
  },
  settleLedger(id, data) {
    return request.post(`/transfer/ledger/${id}/settle`, data)
  }
}

export const archiveApi = {
  getDetail(lightGroupId) {
    return request.get(`/light-group/${lightGroupId}/archive`)
  },
  checkReferences(lightGroupId) {
    return request.get(`/light-group/${lightGroupId}/archive/references`)
  },
  submit(lightGroupId, data) {
    return request.post(`/light-group/${lightGroupId}/archive/submit`, data)
  },
  resubmit(lightGroupId, data) {
    return request.post(`/light-group/${lightGroupId}/archive/resubmit`, data)
  },
  reviewReturn(lightGroupId, data) {
    return request.post(`/light-group/${lightGroupId}/archive/return`, data)
  },
  confirm(lightGroupId, data) {
    return request.post(`/light-group/${lightGroupId}/archive/confirm`, data)
  }
}

export const inspectionApi = {
  createBatch(data) {
    return request.post('/inspection/batch', data)
  },
  getBatchPage(params) {
    return request.get('/inspection/batch/page', { params })
  },
  getBatchDetail(id) {
    return request.get(`/inspection/batch/${id}`)
  },
  saveItems(id, data) {
    return request.put(`/inspection/batch/${id}/items`, data)
  },
  submitBatch(id, data) {
    return request.post(`/inspection/batch/${id}/submit`, data || {})
  }
}

export const anomalyApi = {
  getPage(params) {
    return request.get('/inspection-anomaly/page', { params })
  },
  getDetail(id) {
    return request.get(`/inspection-anomaly/${id}`)
  },
  doReturn(id, data) {
    return request.post(`/inspection-anomaly/${id}/return`, data)
  },
  supplement(id, data) {
    return request.post(`/inspection-anomaly/${id}/supplement`, data)
  },
  confirm(id, data) {
    return request.post(`/inspection-anomaly/${id}/confirm`, data)
  },
  resolve(id, data) {
    return request.post(`/inspection-anomaly/${id}/resolve`, data)
  },
  close(id, data) {
    return request.post(`/inspection-anomaly/${id}/close`, data)
  }
}