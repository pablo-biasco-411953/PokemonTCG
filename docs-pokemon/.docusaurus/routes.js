import React from 'react';
import ComponentCreator from '@docusaurus/ComponentCreator';

export default [
  {
    path: '/blog',
    component: ComponentCreator('/blog', '98b'),
    exact: true
  },
  {
    path: '/markdown-page',
    component: ComponentCreator('/markdown-page', '53a'),
    exact: true
  },
  {
    path: '/docs',
    component: ComponentCreator('/docs', 'c5c'),
    routes: [
      {
        path: '/docs',
        component: ComponentCreator('/docs', 'f86'),
        routes: [
          {
            path: '/docs',
            component: ComponentCreator('/docs', '8c5'),
            routes: [
              {
                path: '/docs/algoritmos/batalla-ia',
                component: ComponentCreator('/docs/algoritmos/batalla-ia', '3fe'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/algoritmos/matchmaking',
                component: ComponentCreator('/docs/algoritmos/matchmaking', 'f06'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/algoritmos/selector-cartas',
                component: ComponentCreator('/docs/algoritmos/selector-cartas', '6bf'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/algoritmos/validador-mazos',
                component: ComponentCreator('/docs/algoritmos/validador-mazos', '27f'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/category/️-arquitectura-técnica',
                component: ComponentCreator('/docs/category/️-arquitectura-técnica', '002'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/category/-componentes-detallados',
                component: ComponentCreator('/docs/category/-componentes-detallados', 'c45'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/category/-jugabilidad--reglas',
                component: ComponentCreator('/docs/category/-jugabilidad--reglas', '88b'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/config/cors-config',
                component: ComponentCreator('/docs/componentes-detallados/backend/config/cors-config', 'c57'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/config/data-loader',
                component: ComponentCreator('/docs/componentes-detallados/backend/config/data-loader', 'fc3'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/config/websocket-config',
                component: ComponentCreator('/docs/componentes-detallados/backend/config/websocket-config', 'f6a'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/controllers/auth-controller',
                component: ComponentCreator('/docs/componentes-detallados/backend/controllers/auth-controller', '662'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/controllers/battle-controller',
                component: ComponentCreator('/docs/componentes-detallados/backend/controllers/battle-controller', 'd08'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/controllers/card-controller',
                component: ComponentCreator('/docs/componentes-detallados/backend/controllers/card-controller', '613'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/controllers/jugador-controller',
                component: ComponentCreator('/docs/componentes-detallados/backend/controllers/jugador-controller', 'b05'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/controllers/mazo-controller',
                component: ComponentCreator('/docs/componentes-detallados/backend/controllers/mazo-controller', '95b'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/controllers/sobre-controller',
                component: ComponentCreator('/docs/componentes-detallados/backend/controllers/sobre-controller', 'd3e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/models/card-entity',
                component: ComponentCreator('/docs/componentes-detallados/backend/models/card-entity', '029'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/models/enums-commands',
                component: ComponentCreator('/docs/componentes-detallados/backend/models/enums-commands', '16f'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/models/jugador-entity',
                component: ComponentCreator('/docs/componentes-detallados/backend/models/jugador-entity', 'cd9'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/models/mazo-entity',
                component: ComponentCreator('/docs/componentes-detallados/backend/models/mazo-entity', 'ff9'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/models/partida-battle-models',
                component: ComponentCreator('/docs/componentes-detallados/backend/models/partida-battle-models', '75d'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/repositories/repositories-overview',
                component: ComponentCreator('/docs/componentes-detallados/backend/repositories/repositories-overview', '2bf'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/services/auth-service',
                component: ComponentCreator('/docs/componentes-detallados/backend/services/auth-service', '26e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/services/battle-engine-service',
                component: ComponentCreator('/docs/componentes-detallados/backend/services/battle-engine-service', 'ec1'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/services/card-catalog-service',
                component: ComponentCreator('/docs/componentes-detallados/backend/services/card-catalog-service', '62b'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/services/lobby-room-service',
                component: ComponentCreator('/docs/componentes-detallados/backend/services/lobby-room-service', 'b44'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/services/mazo-service',
                component: ComponentCreator('/docs/componentes-detallados/backend/services/mazo-service', '76b'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/backend/services/sobre-service',
                component: ComponentCreator('/docs/componentes-detallados/backend/services/sobre-service', '6d3'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/components/apertura-sobre',
                component: ComponentCreator('/docs/componentes-detallados/frontend/components/apertura-sobre', 'f35'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/components/battle-board',
                component: ComponentCreator('/docs/componentes-detallados/frontend/components/battle-board', '202'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/components/deck-builder',
                component: ComponentCreator('/docs/componentes-detallados/frontend/components/deck-builder', 'e8f'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/components/lobby',
                component: ComponentCreator('/docs/componentes-detallados/frontend/components/lobby', '95e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/components/shared-models',
                component: ComponentCreator('/docs/componentes-detallados/frontend/components/shared-models', '4dc'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/hooks-utils/hooks-utilities',
                component: ComponentCreator('/docs/componentes-detallados/frontend/hooks-utils/hooks-utilities', '2e8'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/auth-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/auth-service', 'efa'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/battle-board-action-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/battle-board-action-service', '4c5'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/battle-board-attack-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/battle-board-attack-service', '59c'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/battle-board-combat-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/battle-board-combat-service', 'a61'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/battle-board-state-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/battle-board-state-service', '26e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/battle-board-turn-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/battle-board-turn-service', '89e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/battle-board-ui-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/battle-board-ui-service', '896'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/battle-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/battle-service', '5f4'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/card-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/card-service', '162'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/jugador-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/jugador-service', '5d2'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/lobby-room-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/lobby-room-service', '176'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/mazo-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/mazo-service', 'd6d'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/services/sobre-service',
                component: ComponentCreator('/docs/componentes-detallados/frontend/services/sobre-service', 'e47'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/types/battle-board-types',
                component: ComponentCreator('/docs/componentes-detallados/frontend/types/battle-board-types', 'efd'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/types/battle-types',
                component: ComponentCreator('/docs/componentes-detallados/frontend/types/battle-types', 'de2'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/types/card-types',
                component: ComponentCreator('/docs/componentes-detallados/frontend/types/card-types', 'bf8'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/types/jugador-types',
                component: ComponentCreator('/docs/componentes-detallados/frontend/types/jugador-types', 'fd4'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/types/lobby-types',
                component: ComponentCreator('/docs/componentes-detallados/frontend/types/lobby-types', '100'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/types/mazo-types',
                component: ComponentCreator('/docs/componentes-detallados/frontend/types/mazo-types', '9f0'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/componentes-detallados/frontend/types/models',
                component: ComponentCreator('/docs/componentes-detallados/frontend/types/models', 'c01'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/diagramas/arquitectura-general',
                component: ComponentCreator('/docs/diagramas/arquitectura-general', '4e6'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/diagramas/componentes-dependencias',
                component: ComponentCreator('/docs/diagramas/componentes-dependencias', 'e3e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/diagramas/flujo-autenticacion',
                component: ComponentCreator('/docs/diagramas/flujo-autenticacion', '6a1'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/diagramas/flujo-batalla',
                component: ComponentCreator('/docs/diagramas/flujo-batalla', '4c7'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/diagramas/flujo-websocket',
                component: ComponentCreator('/docs/diagramas/flujo-websocket', 'dda'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/diagramas/modelo-datos',
                component: ComponentCreator('/docs/diagramas/modelo-datos', 'e17'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/glosario',
                component: ComponentCreator('/docs/glosario', '132'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/intro',
                component: ComponentCreator('/docs/intro', '61d'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/jugabilidad/batalla-reglas',
                component: ComponentCreator('/docs/jugabilidad/batalla-reglas', '2ec'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/jugabilidad/cartas-tipos-energia',
                component: ComponentCreator('/docs/jugabilidad/cartas-tipos-energia', 'e41'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/jugabilidad/construccion-mazos',
                component: ComponentCreator('/docs/jugabilidad/construccion-mazos', '934'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/jugabilidad/efectos-habilidades',
                component: ComponentCreator('/docs/jugabilidad/efectos-habilidades', 'f7b'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/jugabilidad/evolucion-pokemon',
                component: ComponentCreator('/docs/jugabilidad/evolucion-pokemon', '946'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/jugabilidad/fases-turno',
                component: ComponentCreator('/docs/jugabilidad/fases-turno', 'e57'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/jugabilidad/items-equipamiento',
                component: ComponentCreator('/docs/jugabilidad/items-equipamiento', 'faa'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/jugabilidad/mecanicas-basicas',
                component: ComponentCreator('/docs/jugabilidad/mecanicas-basicas', '860'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/jugabilidad/overview-juego',
                component: ComponentCreator('/docs/jugabilidad/overview-juego', '508'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/jugabilidad/sobres-booster',
                component: ComponentCreator('/docs/jugabilidad/sobres-booster', 'e94'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/operaciones/database-setup',
                component: ComponentCreator('/docs/operaciones/database-setup', '94e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/operaciones/docker-deployment',
                component: ComponentCreator('/docs/operaciones/docker-deployment', '052'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/operaciones/performance-tips',
                component: ComponentCreator('/docs/operaciones/performance-tips', '4e7'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/operaciones/scripts-utiles',
                component: ComponentCreator('/docs/operaciones/scripts-utiles', '40e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/operaciones/setup-local',
                component: ComponentCreator('/docs/operaciones/setup-local', 'd2f'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/operaciones/troubleshooting',
                component: ComponentCreator('/docs/operaciones/troubleshooting', 'b6a'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/operaciones/variables-entorno',
                component: ComponentCreator('/docs/operaciones/variables-entorno', '015'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/tecnica/algoritmos-clave',
                component: ComponentCreator('/docs/tecnica/algoritmos-clave', 'b7d'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/tecnica/api-endpoints',
                component: ComponentCreator('/docs/tecnica/api-endpoints', '423'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/tecnica/arquitectura-backend',
                component: ComponentCreator('/docs/tecnica/arquitectura-backend', 'c50'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/tecnica/arquitectura-frontend',
                component: ComponentCreator('/docs/tecnica/arquitectura-frontend', 'a0e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/tecnica/autenticacion',
                component: ComponentCreator('/docs/tecnica/autenticacion', '9c0'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/tecnica/batalla-engine',
                component: ComponentCreator('/docs/tecnica/batalla-engine', '2ab'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/tecnica/database-design',
                component: ComponentCreator('/docs/tecnica/database-design', '06b'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/tecnica/manejo-estado',
                component: ComponentCreator('/docs/tecnica/manejo-estado', 'f18'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/tecnica/patrones-diseño',
                component: ComponentCreator('/docs/tecnica/patrones-diseño', '368'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/tecnica/stack-tecnologico',
                component: ComponentCreator('/docs/tecnica/stack-tecnologico', 'd2f'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/docs/tecnica/websocket-lobby',
                component: ComponentCreator('/docs/tecnica/websocket-lobby', '38b'),
                exact: true,
                sidebar: "tutorialSidebar"
              }
            ]
          }
        ]
      }
    ]
  },
  {
    path: '/',
    component: ComponentCreator('/', 'e5f'),
    exact: true
  },
  {
    path: '*',
    component: ComponentCreator('*'),
  },
];
