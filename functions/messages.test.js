const { test } = require('node:test');
const assert = require('node:assert/strict');
const { buildMessages, text } = require('./messages');

const group = { name: 'Casa', memberIds: ['ana', 'bea', 'carla', 'dani'] };
const movement = { type: 'EXPENSE', amountCents: 1250, description: 'Mercadona', createdById: 'ana', createdByName: 'Ana' };

test('avisa a los demás miembros, no a quien lo añadió ni a quien silenció el grupo', () => {
  const users = {
    ana: { fcmTokens: ['t-ana'] },
    bea: { fcmTokens: ['t-bea1', 't-bea2', 't-bea1'], lang: 'es' },
    carla: { fcmTokens: ['t-carla'], mutedGroups: ['casa'] },
    // dani no tiene documento (nunca abrió la versión con avisos): no se le envía nada
  };
  const messages = buildMessages('casa', group, 'm1', movement, users);
  assert.deepEqual(messages.map((m) => m.token), ['t-bea1', 't-bea2']);
  assert.deepEqual(messages[0].data, { groupId: 'casa', movementId: 'm1' });
  assert.equal(messages[0].android.notification.channelId, 'movements');
});

test('texto en español e inglés con el importe formateado', () => {
  const es = text(group, movement, 'es');
  assert.equal(es.title, 'Casa');
  assert.match(es.body, /^Ana ha añadido un gasto de 12,50\s€ · Mercadona$/);
  const en = text(group, { ...movement, type: 'INCOME', description: '' }, 'en');
  assert.match(en.body, /^Ana added an income of €12\.50$/);
});
