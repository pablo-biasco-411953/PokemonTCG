# Reglas — Frontend (Angular / TypeScript)

---

## Estructura de features

Cada feature vive en `src/app/features/<nombre>/` y es autocontenida:

```
features/battle/
├── battle-board.component.ts       ← componente principal
├── services/                       ← servicios locales de la feature
├── *.types.ts                      ← tipos e interfaces locales
└── ...
```

- Los servicios compartidos entre features van en `src/app/core/services/`.
- Los modelos compartidos van en `src/app/shared/models/`.
- No importes desde otra feature directamente — usá `core/` o `shared/` como puente.

## TypeScript

- Tipado estricto siempre. No uses `any` salvo casos excepcionales justificados.
- Interfaces para modelos de datos, no `type` aliases.
- Nombrá los observables con sufijo `$`: `cartas$`, `partida$`.

## Componentes Angular

- Un componente = una responsabilidad.
- Usá `OnPush` change detection cuando sea posible para performance.
- No pongas lógica compleja en el template — extraela al componente o al servicio.
- Inputs/Outputs tipados explícitamente.

## Estilos

- El proyecto usa **Tailwind CSS** + SCSS por componente.
- Usá clases de Tailwind para layout y spacing.
- Usá el archivo `.scss` del componente solo para estilos que Tailwind no cubre (animaciones, pseudo-elementos).
- No uses `!important` salvo que no haya alternativa.

## Comunicación con el backend

- Toda llamada HTTP pasa por un servicio en `core/services/` o `features/<nombre>/services/`.
- Nunca hagas HTTP calls desde un componente directamente.
- Manejá errores de HTTP con `catchError` en el pipe del observable.
- La URL base del API está en `core/services/api-config.ts` — no la hardcodees en otros lados.

## i18n

- El proyecto tiene soporte multilenguaje vía `i18n/i18n.service.ts` y `i18n/translate.pipe.ts`.
- Todo texto visible al usuario debe pasar por el pipe de traducción: `{{ 'clave' | translate }}`.
- No hardcodees strings en español o inglés en los templates.
