const DB_NAME = 'crm-behavior-list-cache'
const DB_VERSION = 1
const STORE = 'snapshots'

let dbPromise

function openDb() {
  if (dbPromise) {
    return dbPromise
  }
  dbPromise = new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    request.onupgradeneeded = () => {
      request.result.createObjectStore(STORE, { keyPath: 'cacheKey' })
    }
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
  return dbPromise
}

function requestToPromise(request) {
  return new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

/** 写入列表快照（IndexedDB，支持大量行） */
export async function saveBehaviorListCache(cacheKey, snapshot) {
  if (!cacheKey || !snapshot?.list?.length) {
    return
  }
  try {
    const db = await openDb()
    const tx = db.transaction(STORE, 'readwrite')
    tx.objectStore(STORE).put({
      cacheKey,
      list: snapshot.list,
      lastId: snapshot.lastId ?? 0,
      hasMore: !!snapshot.hasMore,
      dbTotal: snapshot.dbTotal ?? 0,
      savedAt: Date.now()
    })
    await new Promise((resolve, reject) => {
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
    })
  } catch (e) {
    console.warn('[behavior cache] save failed', e)
  }
}

/** 读取列表快照 */
export async function readBehaviorListCache(cacheKey) {
  if (!cacheKey) {
    return null
  }
  try {
    const db = await openDb()
    const tx = db.transaction(STORE, 'readonly')
    const record = await requestToPromise(tx.objectStore(STORE).get(cacheKey))
    if (!record?.list?.length) {
      return null
    }
    return record
  } catch (e) {
    console.warn('[behavior cache] read failed', e)
    return null
  }
}

export async function clearBehaviorListCache(cacheKey) {
  if (!cacheKey) {
    return
  }
  try {
    const db = await openDb()
    const tx = db.transaction(STORE, 'readwrite')
    tx.objectStore(STORE).delete(cacheKey)
    await new Promise((resolve, reject) => {
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
    })
  } catch (e) {
    console.warn('[behavior cache] clear failed', e)
  }
}

/** 仅存接口字段，减小体积 */
export function compactBehaviorRows(rows) {
  return rows.map(row => ({
    id: row.id,
    customerId: row.customerId,
    behaviorType: row.behaviorType,
    description: row.description,
    behaviorTime: row.behaviorTime
  }))
}
