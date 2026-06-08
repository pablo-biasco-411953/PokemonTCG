import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
  title: '⚡ Pokémon TCG',
  tagline: 'Documentación Oficial - Gotta Document \'Em All!',
  favicon: 'img/pokeball.ico',

  // Future flags, see https://docusaurus.io/docs/api/docusaurus-config#future
  future: {
    v4: true, // Improve compatibility with the upcoming Docusaurus v4
  },

  // Set the production url of your site here
  url: 'https://pokemontcg-docs.example.com',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'pokemontcg', // Usually your GitHub org/user name.
  projectName: 'pokemontcg', // Usually your repo name.

  onBrokenLinks: 'warn', // Permite broken links para desarrollo, cambia a 'throw' en producción

  // Configuración de idioma - Español
  i18n: {
    defaultLocale: 'es',
    locales: ['es'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/',
        },
        blog: {
          showReadingTime: true,
          feedOptions: {
            type: ['rss', 'atom'],
            xslt: true,
          },
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/',
          // Useful options to enforce blogging best practices
          onInlineTags: 'warn',
          onInlineAuthors: 'warn',
          onUntruncatedBlogPosts: 'warn',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    // Replace with your project's social card
    image: 'img/docusaurus-social-card.jpg',
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: '⚡ Pokémon TCG Docs',
      logo: {
        alt: 'Pokémon TCG Logo',
        src: 'img/pokeball.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'tutorialSidebar',
          position: 'left',
          label: '📚 Documentación',
        },
        {
          to: '/docs/glosario',
          label: '📖 Glosario',
          position: 'left',
        },
        {
          href: 'https://github.com/pokemontcg/pokemontcg',
          label: '🔗 GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: '🎮 Jugabilidad',
          items: [
            {
              label: 'Reglas del Juego',
              to: '/docs/jugabilidad/overview-juego',
            },
            {
              label: 'Mecánicas Básicas',
              to: '/docs/jugabilidad/mecanicas-basicas',
            },
            {
              label: 'Constructor de Mazos',
              to: '/docs/jugabilidad/construccion-mazos',
            },
          ],
        },
        {
          title: '⚙️ Técnica',
          items: [
            {
              label: 'Stack Tecnológico',
              to: '/docs/tecnica/stack-tecnologico',
            },
            {
              label: 'Arquitectura',
              to: '/docs/tecnica/arquitectura-backend',
            },
            {
              label: 'API Endpoints',
              to: '/docs/tecnica/api-endpoints',
            },
          ],
        },
        {
          title: '🚀 Operaciones',
          items: [
            {
              label: 'Setup Local',
              to: '/docs/operaciones/setup-local',
            },
            {
              label: 'Docker Deployment',
              to: '/docs/operaciones/docker-deployment',
            },
            {
              label: 'Troubleshooting',
              to: '/docs/operaciones/troubleshooting',
            },
          ],
        },
      ],
      copyright: `© ${new Date().getFullYear()} Pokémon TCG. Construido con Docusaurus ⚡`,
    },
    // Colores temáticos Pokémon
    colorMode: {
      defaultMode: 'light',
      respectPrefersColorScheme: true,
    },
    // Configuración de elementos Pokémon
    docs: {
      sidebar: {
        hideable: true,
        autoCollapseCategories: true,
      },
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'typescript', 'bash', 'sql'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
