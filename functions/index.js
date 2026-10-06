// Cloud Functions de MiGasto. Despliegue: firebase deploy --only functions
const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const { logger } = require('firebase-functions');
const { initializeApp } = require('firebase-admin/app');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');
const { buildMessages } = require('./messages');

initializeApp();

// Tokens que ya no sirven (app desinstalada, datos borrados…): se quitan para no reintentarlos.
const DEAD_TOKEN = new Set(['messaging/registration-token-not-registered', 'messaging/invalid-registration-token']);

/** Avisa al resto de miembros del grupo cuando alguien añade un movimiento. */
exports.notifyNewMovement = onDocumentCreated(
  // La base de datos está en eur3: la función debe estar en una región europea compatible.
  { document: 'groups/{groupId}/movements/{movementId}', region: 'europe-west1' },
  async (event) => {
    const movement = event.data?.data();
    if (!movement) return;
    const { groupId, movementId } = event.params;
    const db = getFirestore();

    const groupSnap = await db.doc(`groups/${groupId}`).get();
    if (!groupSnap.exists) return;
    const group = groupSnap.data();
    const others = (group.memberIds || []).filter((uid) => uid !== movement.createdById);
    if (others.length === 0) return;

    const userSnaps = await db.getAll(...others.map((uid) => db.doc(`users/${uid}`)));
    const users = Object.fromEntries(userSnaps.filter((s) => s.exists).map((s) => [s.id, s.data()]));
    const messages = buildMessages(groupId, group, movementId, movement, users);
    if (messages.length === 0) return;

    const result = await getMessaging().sendEach(messages.map(({ uid, ...message }) => message));
    const cleanups = [];
    result.responses.forEach((response, i) => {
      const code = response.error?.code;
      if (code && DEAD_TOKEN.has(code)) {
        const { uid, token } = messages[i];
        cleanups.push(db.doc(`users/${uid}`).update({ fcmTokens: FieldValue.arrayRemove(token) }).catch(() => {}));
      } else if (code) {
        logger.warn('No se pudo enviar un aviso', { code });
      }
    });
    await Promise.all(cleanups);
    logger.info('Avisos enviados', { groupId, sent: result.successCount, failed: result.failureCount });
  },
);
