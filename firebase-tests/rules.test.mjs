// Pruebas de firestore.rules. Ejecutar con: npm test (requiere firebase-tools y Java).
import { after, before, beforeEach, describe, test } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  arrayRemove,
  arrayUnion,
  Bytes,
  collection,
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from 'firebase/firestore';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-migasto',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => env?.cleanup());
beforeEach(async () => env.clearFirestore());

const db = (uid) => env.authenticatedContext(uid).firestore();
const anon = () => env.unauthenticatedContext().firestore();

async function createGroup(uid = 'emil', id = 'casa', code = 'ABC234') {
  const store = db(uid);
  const batch = writeBatch(store);
  batch.set(doc(store, 'groups', id), {
    name: 'Casa',
    icon: 'HOME',
    inviteCode: code,
    ownerId: uid,
    memberIds: [uid],
    members: { [uid]: { name: 'Emil', email: 'emil@example.com' } },
    createdAt: 1,
  });
  batch.set(doc(store, 'inviteCodes', code), { groupId: id, createdBy: uid });
  await batch.commit();
}

function movement(uid, extra = {}) {
  return {
    type: 'EXPENSE',
    method: 'BANK',
    amountCents: 4500,
    description: 'Peña',
    categoryId: 'leisure',
    dateEpochDay: 20727,
    createdAt: 10,
    updatedAt: 10,
    createdById: uid,
    createdByName: 'Emil',
    hasPhoto: false,
    ...extra,
  };
}

async function join(uid, code = 'ABC234', groupId = 'casa') {
  const store = db(uid);
  const codeDoc = await getDoc(doc(store, 'inviteCodes', code));
  return updateDoc(doc(store, 'groups', codeDoc.data()?.groupId ?? groupId), {
    memberIds: arrayUnion(uid),
    [`members.${uid}`]: { name: 'Laura', email: 'laura@example.com' },
    joinCode: code,
  });
}

describe('grupos', () => {
  test('crear un grupo con su código', async () => {
    await assertSucceeds(createGroup());
  });

  test('no se puede crear un grupo en nombre de otro', async () => {
    const store = db('intruso');
    await assertFails(setDoc(doc(store, 'groups', 'x'), {
      name: 'X', icon: 'HOME', inviteCode: 'ABC234', ownerId: 'emil',
      memberIds: ['emil'], members: {}, createdAt: 1,
    }));
  });

  test('sin sesión no se puede leer nada', async () => {
    await createGroup();
    await assertFails(getDoc(doc(anon(), 'groups', 'casa')));
    await assertFails(getDoc(doc(anon(), 'inviteCodes', 'ABC234')));
  });

  test('solo los miembros leen el grupo y las consultas por miembro funcionan', async () => {
    await createGroup();
    await assertSucceeds(getDoc(doc(db('emil'), 'groups', 'casa')));
    await assertFails(getDoc(doc(db('laura'), 'groups', 'casa')));
    await assertSucceeds(getDocs(query(collection(db('emil'), 'groups'), where('memberIds', 'array-contains', 'emil'))));
    await assertFails(getDocs(collection(db('laura'), 'groups')));
  });

  test('unirse con el código correcto', async () => {
    await createGroup();
    await assertSucceeds(join('laura'));
    await assertSucceeds(getDoc(doc(db('laura'), 'groups', 'casa')));
  });

  test('no se puede unir con un código incorrecto', async () => {
    await createGroup();
    const store = db('laura');
    await assertFails(updateDoc(doc(store, 'groups', 'casa'), {
      memberIds: arrayUnion('laura'),
      'members.laura': { name: 'Laura' },
      joinCode: 'ZZZZZZ',
    }));
  });

  test('quien se une no puede añadir a otros ni cambiar el nombre', async () => {
    await createGroup();
    const store = db('laura');
    await assertFails(updateDoc(doc(store, 'groups', 'casa'), {
      memberIds: arrayUnion('laura', 'pepe'),
      'members.laura': { name: 'Laura' },
      joinCode: 'ABC234',
    }));
    await assertFails(updateDoc(doc(store, 'groups', 'casa'), {
      memberIds: arrayUnion('laura'),
      'members.laura': { name: 'Laura' },
      joinCode: 'ABC234',
      name: 'Mío',
    }));
  });

  test('los códigos no se pueden listar', async () => {
    await createGroup();
    await assertSucceeds(getDoc(doc(db('laura'), 'inviteCodes', 'ABC234')));
    await assertFails(getDocs(collection(db('laura'), 'inviteCodes')));
  });

  test('no se puede robar el código de otro grupo', async () => {
    await createGroup();
    await assertFails(setDoc(doc(db('laura'), 'inviteCodes', 'ABC234'), { groupId: 'otro' }));
  });

  test('un miembro puede renombrar y salir, pero no cambiar el código', async () => {
    await createGroup();
    await join('laura');
    const store = db('laura');
    await assertSucceeds(updateDoc(doc(store, 'groups', 'casa'), { name: 'Piso' }));
    await assertFails(updateDoc(doc(store, 'groups', 'casa'), { inviteCode: 'QQQQQQ' }));
    await assertSucceeds(updateDoc(doc(store, 'groups', 'casa'), {
      memberIds: arrayRemove('laura'),
      'members.laura': deleteField(),
    }));
    await assertFails(getDoc(doc(store, 'groups', 'casa')));
  });

  test('el último miembro borra el grupo y su código', async () => {
    await createGroup();
    const store = db('emil');
    const batch = writeBatch(store);
    batch.delete(doc(store, 'inviteCodes', 'ABC234'));
    batch.delete(doc(store, 'groups', 'casa'));
    await assertSucceeds(batch.commit());
  });
});

describe('gestión del creador', () => {
  test('el creador quita a un miembro y este deja de ver el grupo', async () => {
    await createGroup();
    await join('laura');
    await assertSucceeds(updateDoc(doc(db('emil'), 'groups', 'casa'), {
      memberIds: arrayRemove('laura'),
      'members.laura': deleteField(),
    }));
    await assertFails(getDoc(doc(db('laura'), 'groups', 'casa')));
  });

  test('un miembro no puede quitar a otro, ni cerrar ni borrar el grupo', async () => {
    await createGroup();
    await join('laura');
    const store = db('laura');
    await assertFails(updateDoc(doc(store, 'groups', 'casa'), {
      memberIds: arrayRemove('emil'),
      'members.emil': deleteField(),
    }));
    await assertFails(updateDoc(doc(store, 'groups', 'casa'), { joinLocked: true }));
    await assertFails(updateDoc(doc(store, 'groups', 'casa'), { ownerId: 'laura' }));
    await assertFails(updateDoc(doc(store, 'inviteCodes', 'ABC234'), { locked: true }));
    await assertFails(deleteDoc(doc(store, 'groups', 'casa')));
  });

  test('con el grupo cerrado nadie más puede unirse', async () => {
    await createGroup();
    const store = db('emil');
    const batch = writeBatch(store);
    batch.update(doc(store, 'groups', 'casa'), { joinLocked: true });
    batch.update(doc(store, 'inviteCodes', 'ABC234'), { locked: true });
    await assertSucceeds(batch.commit());
    await assertFails(join('laura'));
    await assertSucceeds(updateDoc(doc(store, 'groups', 'casa'), { joinLocked: false }));
    await assertSucceeds(join('laura'));
  });

  test('el creador puede salir cediendo el grupo', async () => {
    await createGroup();
    await join('laura');
    await assertSucceeds(updateDoc(doc(db('emil'), 'groups', 'casa'), {
      memberIds: arrayRemove('emil'),
      'members.emil': deleteField(),
      ownerId: 'laura',
    }));
    await assertSucceeds(updateDoc(doc(db('laura'), 'groups', 'casa'), { joinLocked: true }));
  });
});

describe('ajustes de avisos', () => {
  test('cada usuario gestiona solo su documento', async () => {
    await assertSucceeds(setDoc(doc(db('emil'), 'users', 'emil'), { fcmTokens: arrayUnion('t1'), lang: 'es' }, { merge: true }));
    await assertSucceeds(setDoc(doc(db('emil'), 'users', 'emil'), { mutedGroups: arrayUnion('casa') }, { merge: true }));
    await assertSucceeds(getDoc(doc(db('emil'), 'users', 'emil')));
    await assertFails(getDoc(doc(db('laura'), 'users', 'emil')));
    await assertFails(setDoc(doc(db('laura'), 'users', 'emil'), { fcmTokens: ['robado'] }, { merge: true }));
    await assertFails(setDoc(doc(db('emil'), 'users', 'emil'), { admin: true }, { merge: true }));
    await assertSucceeds(deleteDoc(doc(db('emil'), 'users', 'emil')));
  });
});

describe('movimientos', () => {
  test('los miembros crean, editan, leen y borran', async () => {
    await createGroup();
    await join('laura');
    const ref = doc(db('emil'), 'groups/casa/movements/m1');
    await assertSucceeds(setDoc(ref, movement('emil')));
    await assertSucceeds(getDocs(collection(db('laura'), 'groups/casa/movements')));
    await assertSucceeds(setDoc(doc(db('laura'), 'groups/casa/movements/m1'), { amountCents: 5000 }, { merge: true }));
    await assertSucceeds(deleteDoc(doc(db('laura'), 'groups/casa/movements/m1')));
  });

  test('quien no es miembro no ve ni escribe movimientos', async () => {
    await createGroup();
    await setDoc(doc(db('emil'), 'groups/casa/movements/m1'), movement('emil'));
    await assertFails(getDocs(collection(db('laura'), 'groups/casa/movements')));
    await assertFails(setDoc(doc(db('laura'), 'groups/casa/movements/m2'), movement('laura')));
  });

  test('no se puede suplantar al autor ni guardar datos inválidos', async () => {
    await createGroup();
    await join('laura');
    const store = db('laura');
    await assertFails(setDoc(doc(store, 'groups/casa/movements/m1'), movement('emil')));
    await assertFails(setDoc(doc(store, 'groups/casa/movements/m2'), movement('laura', { amountCents: -5 })));
    await assertFails(setDoc(doc(store, 'groups/casa/movements/m3'), movement('laura', { type: 'ROBO' })));
    await assertFails(setDoc(doc(store, 'groups/casa/movements/m4'), movement('laura', { amountCents: 1.5 })));
    await setDoc(doc(db('emil'), 'groups/casa/movements/m5'), movement('emil'));
    await assertFails(setDoc(doc(store, 'groups/casa/movements/m5'), { createdById: 'laura' }, { merge: true }));
  });

  test('fotos de tickets solo para miembros', async () => {
    await createGroup();
    const bytes = Bytes.fromUint8Array(new Uint8Array([0xff, 0xd8, 0xff]));
    await assertSucceeds(setDoc(doc(db('emil'), 'groups/casa/photos/m1'), { data: bytes, createdBy: 'emil' }));
    await assertFails(getDoc(doc(db('laura'), 'groups/casa/photos/m1')));
    await assertFails(setDoc(doc(db('emil'), 'groups/casa/photos/m2'), { data: 'texto' }));
  });
});
