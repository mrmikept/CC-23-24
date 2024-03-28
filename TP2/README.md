# CC TP2 - Peer-to-peer File Transfer System

Trabalho Prático 2 relativo à Unidade Curricular de Comunicações por Computador da Universidade do Minho do ano Letivo 2023/24.

## Objetivo

Este trabalho consiste no desenvolvimento, implementação e teste de um serviço de partilha de ficheiros `peer-to-peer` (P2P) de alto desempenho. Este serviço é composto por uma rede de dispositivos designados de `FS_Nodes` que consistem simultaneamente em serviço cliente/servidor. Cada `FS_Node` conecta-se a um dispositivo de registo de conteúdo, designado por FS_Tracker que regista a localização de todos os ficheiros disponíveis nos `FS_Nodes`. As comunicações entre `FS_Node` e `FS_Tracker` são realizadas mediante um protocolo designado por `FS_Tracker_Protocol` que corre sobre **TCP**. Já as comunicações entre `FS_Nodes` são realizadas mediante o `FS_Transfer_Protocol` que corre sobre **UDP**.

## Conteúdos

Esta pasta contem os seguintes ficheiros relativos ao TP2:

- [Relatório](./report.pdf)
- [Código Fonte](./Code/src/)
- [Enunciado](./enunciado.pdf)
- [Topologia Utilizada](./CC-Topologia.pdf)
- [Ficheiros de configuração do `bind9`](./Code/Bind9%20-%20config/)

## Nota obtida: 20 valores