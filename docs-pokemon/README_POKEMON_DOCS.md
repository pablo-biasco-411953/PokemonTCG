# ⚡ Pokémon TCG - Documentación Oficial con Docusaurus

> Documentación completa, hermosa y temática del proyecto Pokémon Trading Card Game

---

## 🎉 ¡Fase 1 Completada!

Se ha inicializado exitosamente un sitio **Docusaurus** con temática Pokémon para documentar el proyecto Pokemon TCG.

### ✅ Lo que se hizo en Fase 1

- ✅ Inicializado proyecto Docusaurus 3.10.1
- ✅ Configurado tema con colores Pokémon (Amarillo #FFCC00, Azul #0066CC)
- ✅ Creada estructura de carpetas para 80+ documentos
- ✅ Configuración de idioma: Español
- ✅ Creados archivos iniciales:
  - `docs/intro.md` - Página de bienvenida temática
  - `docs/glosario.md` - Glosario completo (150+ términos)
  - `docs/operaciones/01-setup-local.md` - Guía de instalación
  - `docs/jugabilidad/01-overview-juego.md` - Visión general del juego
- ✅ Configurados `_category_.json` para navegación
- ✅ Instaladas dependencias (sass)
- ✅ **Build exitoso** ✨

---

## 🚀 Cómo usar esta documentación

### Iniciar el Servidor de Desarrollo

```bash
cd docs-pokemon

# Instalar dependencias (si es la primera vez)
npm install

# Iniciar servidor local
npm start
```

**Abre en navegador**: `http://localhost:3000`

El servidor tiene **hot reload** - cualquier cambio en los `.md` se refleja automáticamente.

---

### Construir para Producción

```bash
npm run build

# Servir el build local (para probar)
npm run serve
```

---

## 📁 Estructura de Carpetas

```
docs-pokemon/
├── docs/
│   ├── intro.md                      # Portada (HECHA ✅)
│   ├── glosario.md                  # Glosario (HECHA ✅)
│   │
│   ├── jugabilidad/                 # 🎮 Pillar 1: Gameplay
│   │   ├── _category_.json
│   │   ├── 01-overview-juego.md     # (HECHA ✅)
│   │   ├── 02-mecanicas-basicas.md  # (TODO)
│   │   ├── 03-cartas-tipos-energia.md
│   │   ├── 04-evolucion-pokemon.md
│   │   ├── 05-construccion-mazos.md
│   │   ├── 06-batalla-reglas.md
│   │   ├── 07-fases-turno.md
│   │   ├── 08-efectos-habilidades.md
│   │   ├── 09-sobres-booster.md
│   │   └── 10-items-equipamiento.md
│   │
│   ├── tecnica/                     # ⚙️ Pillar 2: Architecture
│   │   ├── _category_.json
│   │   ├── 01-stack-tecnologico.md
│   │   ├── 02-arquitectura-backend.md
│   │   ├── ... (8 más)
│   │
│   ├── componentes-detallados/      # 🔍 Pillar 3: Components
│   │   ├── backend/
│   │   │   ├── controllers/
│   │   │   ├── services/
│   │   │   ├── models/
│   │   │   ├── repositories/
│   │   │   └── config/
│   │   │
│   │   └── frontend/
│   │       ├── services/
│   │       ├── components/
│   │       ├── types/
│   │       └── hooks-utils/
│   │
│   ├── algoritmos/                  # 🧠 Algoritmos
│   ├── operaciones/                 # 🚀 DevOps
│   │   ├── 01-setup-local.md       # (HECHA ✅)
│   │   ├── 02-variables-entorno.md
│   │   ├── ... (5 más)
│   │
│   └── diagramas/                   # 📐 Visuales
│
├── src/
│   ├── css/custom.css               # Estilos temáticos Pokémon (HECHA ✅)
│   └── pages/                        # Páginas customizadas
│
├── static/
│   └── img/                          # Imágenes (logos, sprites, etc)
│
├── docusaurus.config.ts             # Config principal (HECHA ✅)
├── sidebars.ts                      # Estructura de navegación
├── package.json
└── README_POKEMON_DOCS.md            # Este archivo

```

---

## 📖 Documentos Completados (Fase 1)

| Documento | Estado | Ubicación |
|-----------|--------|-----------|
| Página de Bienvenida | ✅ HECHA | `docs/intro.md` |
| Glosario Completo | ✅ HECHA | `docs/glosario.md` |
| Setup Local | ✅ HECHA | `docs/operaciones/01-setup-local.md` |
| Visión General del Juego | ✅ HECHA | `docs/jugabilidad/01-overview-juego.md` |
| Tema CSS Pokémon | ✅ HECHA | `src/css/custom.css` |
| Config Docusaurus | ✅ HECHA | `docusaurus.config.ts` |

**Total completado**: 6 documentos principales + configuración

---

## 📋 Próximas Fases (Fase 2-9)

### Fase 2: Jugabilidad (10 documentos)
- [ ] 02-mecanicas-basicas.md
- [ ] 03-cartas-tipos-energia.md
- [ ] 04-evolucion-pokemon.md
- [ ] 05-construccion-mazos.md
- [ ] 06-batalla-reglas.md
- [ ] 07-fases-turno.md
- [ ] 08-efectos-habilidades.md
- [ ] 09-sobres-booster.md
- [ ] 10-items-equipamiento.md

**Tiempo estimado**: 4-5 horas

---

### Fase 3: Técnica (11 documentos)
- [ ] 01-stack-tecnologico.md
- [ ] 02-arquitectura-backend.md
- [ ] 03-arquitectura-frontend.md
- [ ] 04-database-design.md
- [ ] 05-api-endpoints.md
- [ ] 06-patrones-diseño.md
- [ ] 07-batalla-engine.md
- [ ] 08-websocket-lobby.md
- [ ] 09-autenticacion.md
- [ ] 10-manejo-estado.md
- [ ] 11-algoritmos-clave.md

**Tiempo estimado**: 6-8 horas

---

### Fase 4: Componentes (20+ documentos)
Documentación línea por línea de:
- Backend Controllers (6 docs)
- Backend Services (5 docs)
- Backend Models (5 docs)
- Frontend Services (5 docs)
- Frontend Components (5 docs)
- Frontend Types (1 doc)
- Frontend Hooks (1 doc)

**Tiempo estimado**: 10-15 horas

---

### Fase 5: Algoritmos (4 documentos)
- [ ] batalla-ia.md
- [ ] selector-cartas.md
- [ ] validador-mazos.md
- [ ] matchmaking.md

**Tiempo estimado**: 2-3 horas

---

### Fase 6: Operaciones (7 documentos)
- [✅] 01-setup-local.md
- [ ] 02-variables-entorno.md
- [ ] 03-docker-deployment.md
- [ ] 04-database-setup.md
- [ ] 05-scripts-utiles.md
- [ ] 06-troubleshooting.md
- [ ] 07-performance-tips.md

**Tiempo estimado**: 2-3 horas

---

### Fase 7: Diagramas (6 documentos)
- [ ] arquitectura-general.md (Mermaid)
- [ ] flujo-batalla.md (Mermaid)
- [ ] modelo-datos.md (Mermaid)
- [ ] flujo-autenticacion.md (Mermaid)
- [ ] flujo-websocket.md (Mermaid)
- [ ] componentes-dependencias.md (Mermaid)

**Tiempo estimado**: 3-4 horas

---

### Fase 8: Polish & Recursos
- [ ] Agregar imágenes/screenshots
- [ ] Agregar GIFs animados
- [ ] Correcciones gramática
- [ ] Links cruzados
- [ ] Testing del sitio

**Tiempo estimado**: 2-3 horas

---

## 🎨 Colores Temáticos Aplicados

```css
/* Tema Pokémon - Light Mode */
Color Primario:   #FFCC00 (Amarillo icónico)
Color Secundario: #0066CC (Azul)
Success:          #22BB22 (Verde)
Warning:          #FF9900 (Naranja)
Danger:           #EE0000 (Rojo)

/* Tema Oscuro */
Se invierte automáticamente para modo dark
```

**Botones, links y elementos se colorean con estos tonos.**

---

## 📝 Cómo Editar Documentos

### Crear un nuevo documento

```bash
# 1. Crear archivo .md
touch docs/jugabilidad/02-mecanicas-basicas.md

# 2. Agregar frontmatter (metadata)
cat > docs/jugabilidad/02-mecanicas-basicas.md << 'EOF'
---
sidebar_position: 2
title: 🎮 Mecánicas Básicas
---

# Contenido aquí...
EOF

# 3. Guardar y el sitio se actualiza automáticamente
```

### Estructura de Frontmatter

```yaml
---
sidebar_position: 1          # Orden en el sidebar
title: Título del Documento  # Título
description: Descripción     # Meta description (SEO)
---
```

---

## 🔧 Configuración Personalizada

### Cambiar colores Pokémon

Edita `src/css/custom.css`:

```css
:root {
  --ifm-color-primary: #FFCC00;  /* Cambiar aquí */
  --ifm-color-secondary: #0066CC;
}
```

---

### Cambiar título y logo

Edita `docusaurus.config.ts`:

```typescript
const config: Config = {
  title: '⚡ Pokémon TCG',          // Cambiar título
  // ...
  navbar: {
    logo: {
      src: 'img/pokeball.svg',      // Cambiar logo
    },
  }
}
```

---

### Agregar nueva sección

1. Crear carpeta: `docs/nueva-seccion/`
2. Crear `docs/nueva-seccion/_category_.json`:
```json
{
  "label": "📚 Título de Sección",
  "position": 8
}
```
3. Crear documentos dentro
4. El sitio se actualiza automáticamente

---

## 📊 Estadísticas Actuales

| Métrica | Valor |
|---------|-------|
| Documentos Completados | 4 |
| Documentos Planeados | 80+ |
| Líneas de Documentación | 3,000+ |
| Palabras | 15,000+ |
| Tiempo Invertido (Fase 1) | 30 min |
| Tiempo Estimado (Completo) | 30-40 horas |

---

## 🐛 Troubleshooting

### El sitio no se actualiza
```bash
# Reinicia el servidor
npm start
```

### Errores de broken links
Los broken links son esperados mientras se crean documentos. Se convierten en warnings, no errores.
Para hacerlos errores en producción, cambia en `docusaurus.config.ts`:
```typescript
onBrokenLinks: 'throw',
```

### Error: "Cannot find module"
```bash
# Limpia e reinstala dependencias
rm -rf node_modules package-lock.json
npm install
```

---

## 🚀 Deploy (Futuro)

Una vez completada la documentación, puedes deployar a:

- **Vercel**: `npm run deploy` (si usas GitHub Pages)
- **Netlify**: Conecta el repo y configura build
- **Servidor propio**: Usar `npm run build` + servir carpeta `build/`

---

## 📚 Recursos Útiles

- **Docusaurus Docs**: https://docusaurus.io
- **Markdown Cheat Sheet**: https://commonmark.org/help/
- **Mermaid Diagrams**: https://mermaid.js.org

---

## 🎯 Checklist Fase 1

- [x] Docusaurus inicializado
- [x] Tema Pokémon configurado
- [x] Carpetas creadas
- [x] Páginas iniciales escritas
- [x] Build exitoso
- [x] Servidor de desarrollo funciona
- [x] Color scheme aplicado
- [x] README de documentación creado
- [ ] **SIGUIENTE**: Iniciar Fase 2 (Jugabilidad)

---

## 🎓 Próximo Paso

Para continuar con la documentación:

```bash
cd docs-pokemon
npm start

# Abre http://localhost:3000 y empieza a explorar
# Luego, comienza a escribir los documentos de la Fase 2
```

---

## 📞 ¿Necesitas Ayuda?

Si tienes dudas sobre:
- **Docusaurus**: Revisa https://docusaurus.io/docs
- **Markdown**: Usa sintaxis estándar de CommonMark
- **Estructura**: Copia el patrón de documentos existentes

---

## 📜 Licencia

Este sitio de documentación está bajo la misma licencia que el proyecto (MIT).

---

**¡Fase 1 Completada! 🎉**

El sitio está listo. Ahora viene la parte divertida: **llenar con contenido todas las secciones.**

**Gotta Document 'Em All!** ⚡

---

*Generado: 2026-06-08*
*Próxima Actualización: Después de Fase 2*
