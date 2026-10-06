// Construye los avisos de un movimiento nuevo (sin dependencias de Firebase, para poder probarlo).

const TYPES = {
  es: { EXPENSE: 'un gasto', INCOME: 'un ingreso', BILL: 'una factura', TRANSFER: 'un traspaso' },
  en: { EXPENSE: 'an expense', INCOME: 'an income', BILL: 'a bill', TRANSFER: 'a transfer' },
};

function money(cents, lang) {
  return new Intl.NumberFormat(lang === 'en' ? 'en-IE' : 'es-ES', { style: 'currency', currency: 'EUR' })
    .format((Number(cents) || 0) / 100);
}

/** Texto del aviso en el idioma de quien lo recibe ("es" por defecto). */
function text(group, movement, lang) {
  const l = lang === 'en' ? 'en' : 'es';
  const who = String(movement.createdByName || '').trim() || (l === 'en' ? 'Someone' : 'Alguien');
  const what = TYPES[l][movement.type] || TYPES[l].EXPENSE;
  const amount = money(movement.amountCents, l);
  const detail = String(movement.description || '').trim();
  const body = l === 'en' ? `${who} added ${what} of ${amount}` : `${who} ha añadido ${what} de ${amount}`;
  return { title: String(group.name || 'MiGasto'), body: detail ? `${body} · ${detail}` : body };
}

/**
 * Un mensaje por dispositivo de cada miembro que debe enterarse: todos menos quien lo añadió
 * y quien haya silenciado el grupo. [users] es un mapa uid → documento users/{uid}.
 */
function buildMessages(groupId, group, movementId, movement, users) {
  const messages = [];
  for (const uid of group.memberIds || []) {
    if (uid === movement.createdById) continue;
    const user = users[uid];
    if (!user || (user.mutedGroups || []).includes(groupId)) continue;
    const { title, body } = text(group, movement, user.lang);
    for (const token of new Set(user.fcmTokens || [])) {
      messages.push({
        uid,
        token,
        notification: { title, body },
        data: { groupId, movementId },
        android: {
          priority: 'high',
          notification: { channelId: 'movements', icon: 'ic_stat_migasto', color: '#4F46E5' },
        },
      });
    }
  }
  return messages;
}

module.exports = { buildMessages, text };
