#!/usr/bin/env python3
"""
rq5_readers.py — File readers for RQ5 Test Suite Redundancy Analysis - v2 FIXED
=================================================================================
FIXES in v2:
  1. L2 now strips numeric suffixes from event names (was missing, causing
     artificially inflated edge counts and deflated Jaccard similarities)
  2. Clearer documentation of what sequences and edges represent per level

All readers return a common representation:
  - sequences: set of tuples (each tuple is a sequence of event names/tokens)
  - edges: set of (event_a, event_b) tuples (always base event names)
"""

import os
import re
import xml.etree.ElementTree as ET
from collections import OrderedDict
from pathlib import Path

# =============================================================================
# SPL NAME MAPPING
# =============================================================================

SPL_NAME_MAPPING = {
    #"SodaVendingMachine": "SVM",
    #"eMail": "eM",
    #"Elevator": "El",
    #"BankAccountv2": "BAv2",
    #"StudentAttendanceSystem": "SAS",
    "Tesla": "Te",
    "syngovia": "Svia",
    "HockertyShirts": "HS"
}


# =============================================================================
# CONFIGURATION READER
# =============================================================================

def read_product_configurations(config_path):
    """
    Reads product configuration file.
    Format: P0001: <feat1, feat2, feat3>:3 features

    Returns: OrderedDict { product_name: set_of_active_features }
    """
    configs = OrderedDict()
    with open(config_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('Total') or line.startswith('--'):
                continue
            match = re.match(r'^(P\d+):\s*<(.+?)>:\d+\s*features?', line)
            if match:
                product_name = match.group(1)
                features = {feat.strip() for feat in match.group(2).split(',')}
                configs[product_name] = features
    return configs


# =============================================================================
# ESG-Fx TEST SUITE READERS
# =============================================================================

def _strip_numeric_suffix(name):
    """Strips trailing _N from event names used in L2/L3/L4 files."""
    name = name.strip()
    match = re.match(r'^(.+)_(\d+)$', name)
    return match.group(1) if match else name


def _extract_edges_from_event_list(events):
    """Returns set of (event_a, event_b) consecutive pairs."""
    edges = set()
    for i in range(len(events) - 1):
        edges.add((events[i], events[i + 1]))
    return edges


def read_esgfx_test_suite(filepath, level):
    """
    Reads an ESG-Fx test suite file (L0, L1, L2, L3, or L4).

    Format by level:
      L0: "N : eventA, eventB, eventC"           (plain event names)
      L1: "N : eventA, eventB, eventC"           (plain event names)
      L2: "N : eventA_id, eventB_id, eventC_id"  (events with numeric suffixes)
      L3: "N : eA_id:eB_id, eB_id:eC_id"         (edge-pair tokens with suffixes)
      L4: "N : eA_id:eB_id:eC_id, eB_id:eC_id:eD_id" (triple tokens with suffixes)

    Returns:
      sequences: set of tuples (the sequence representation at this level)
      edges: set of (event_a, event_b) tuples (always base event names, suffixes stripped)
    """
    sequences = set()
    edges = set()

    if not os.path.exists(filepath):
        return sequences, edges

    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or re.match(r'^(L\d|Edge|Event|Random)', line):
                continue

            parts = line.split(' : ', 1)
            if len(parts) < 2:
                continue

            seq_data = parts[1].strip()
            if not seq_data:
                continue

            if level in ('L0', 'L1'):
                # Plain event names, no suffixes
                events = [e.strip() for e in seq_data.split(', ') if e.strip()]
                seq_key = tuple(events)
                sequences.add(seq_key)
                edges |= _extract_edges_from_event_list(events)

            elif level == 'L2':
                # FIX: L2 has numeric suffixes that must be stripped
                # File: "get_balance_1, select_deposit_2, enter_deposit_amount_3, put_money_4"
                # Strip suffixes for both sequence keys and edge extraction
                events_raw = [e.strip() for e in seq_data.split(', ') if e.strip()]
                events_clean = [_strip_numeric_suffix(e) for e in events_raw]
                seq_key = tuple(events_clean)
                sequences.add(seq_key)
                edges |= _extract_edges_from_event_list(events_clean)

            elif level in ('L3', 'L4'):
                # Colon-separated tokens (edge-pairs for L3, triples for L4)
                # Each token has numeric suffixes on event names
                tokens = [t.strip() for t in seq_data.split(', ') if t.strip()]

                normalized_tokens = []

                for idx, token in enumerate(tokens):
                    event_parts = [_strip_numeric_suffix(p) for p in token.split(':')]
                    normalized_tokens.append(':'.join(event_parts))

                    # Extract individual edges from within the token
                    for i in range(len(event_parts) - 1):
                        edges.add((event_parts[i], event_parts[i + 1]))

                seq_key = tuple(normalized_tokens)
                sequences.add(seq_key)

    return sequences, edges


def discover_esgfx_suites(base_path, approach):
    """
    Discovers and loads all ESG-Fx product test suites for a given approach.

    Tries both folder name and file prefix patterns.
    """
    suites = {}
    l_num = approach.split('_L')[1]
    level = f'L{l_num}'
    folder = Path(base_path) / 'testsequences' / level

    if not folder.is_dir():
        return suites

    base_path = Path(base_path)
    spl_folder_name = base_path.name
    spl_file_prefix = SPL_NAME_MAPPING.get(spl_folder_name, spl_folder_name)

    if l_num == '0':
        patterns = [
            re.compile(rf'^(P\d+)_RandomWalk\.txt$'),
            re.compile(rf'^{re.escape(spl_file_prefix)}_(P\d+)_RandomWalk\.txt$'),
            re.compile(rf'^{re.escape(spl_folder_name)}_(P\d+)_RandomWalk\.txt$'),
        ]
    else:
        patterns = [
            re.compile(rf'^(P\d+)_L{l_num}\.txt$'),
            re.compile(rf'^{re.escape(spl_file_prefix)}_(P\d+)_L{l_num}\.txt$'),
            re.compile(rf'^{re.escape(spl_folder_name)}_(P\d+)_L{l_num}\.txt$'),
        ]

    for fname in sorted(os.listdir(folder)):
        product = None

        for pattern in patterns:
            match = pattern.match(fname)
            if match:
                product = match.group(1)
                break

        if product:
            filepath = folder / fname
            seqs, edgs = read_esgfx_test_suite(filepath, level)
            if seqs or edgs:
                suites[product] = (seqs, edgs)

    return suites


# =============================================================================
# EFG / GUITAR TEST SUITE READERS
# =============================================================================

def read_efg_event_mapping(efg_filepath):
    """Reads .EFG XML file and builds EventId -> EventName mapping."""
    mapping = {}
    if not os.path.exists(efg_filepath):
        return mapping

    try:
        tree = ET.parse(efg_filepath)
        root = tree.getroot()
        for event_elem in root.iter('Event'):
            event_id = event_elem.findtext('EventId', '').strip()
            # Try 'Name' first (standard GUITAR EFG format)
            event_name = event_elem.findtext('Name', '').strip()
            if not event_name:
                event_name = event_elem.findtext('n', '').strip()
            if event_id and event_name:
                # Normalize: replace spaces with underscores to match ESG-Fx naming
                mapping[event_id] = event_name.replace(' ', '_')
    except ET.ParseError as e:
        print(f"  Warning: Could not parse EFG file {efg_filepath}: {e}")

    return mapping


def read_guitar_tst_file(tst_filepath, event_mapping):
    """Reads a single GUITAR .tst XML file."""
    events = []
    try:
        tree = ET.parse(tst_filepath)
        root = tree.getroot()
        for step in root.iter('Step'):
            event_id = step.findtext('EventId', '').strip()
            event_name = event_mapping.get(event_id)
            if event_name:
                events.append(event_name)
    except ET.ParseError:
        pass
    return tuple(events)


def read_efg_test_suite(efg_filepath, tst_directory):
    """Reads a complete EFG test suite."""
    sequences = set()
    edges = set()

    mapping = read_efg_event_mapping(efg_filepath)
    if not mapping:
        return sequences, edges

    if not os.path.isdir(tst_directory):
        return sequences, edges

    tst_files = sorted([f for f in os.listdir(tst_directory) if f.endswith('.tst')])

    for fname in tst_files:
        filepath = os.path.join(tst_directory, fname)
        seq = read_guitar_tst_file(filepath, mapping)
        if seq:
            sequences.add(seq)
            edges |= _extract_edges_from_event_list(list(seq))

    return sequences, edges


def discover_efg_suites(base_path, l_level):
    """
    Discovers and loads all EFG/GUITAR product test suites for a given L level.

    Tries both folder name and file prefix patterns.
    """
    suites = {}
    base_path = Path(base_path)
    efg_dir = base_path / 'EFGs'
    tst_base = base_path / 'EFGs' / 'efg_testsequences'
    l_label = f'L{l_level}'

    if not efg_dir.is_dir() or not tst_base.is_dir():
        return suites

    spl_folder_name = base_path.name
    spl_file_prefix = SPL_NAME_MAPPING.get(spl_folder_name, spl_folder_name)

    for product_dir in sorted(os.listdir(tst_base)):
        product_path = tst_base / product_dir / l_label

        efg_patterns = [
            efg_dir / f'{product_dir}.EFG',
            efg_dir / f'{spl_file_prefix}_{product_dir}.EFG',
            efg_dir / f'{spl_folder_name}_{product_dir}.EFG',
        ]

        efg_path = None
        for pattern in efg_patterns:
            if pattern.is_file():
                efg_path = pattern
                break

        if product_path.is_dir() and efg_path:
            seqs, edgs = read_efg_test_suite(efg_path, product_path)
            if seqs or edgs:
                suites[product_dir] = (seqs, edgs)

    return suites


# =============================================================================
# UNIFIED DISCOVERY
# =============================================================================

def discover_all_suites(base_path, approach):
    """Discovers and loads test suites for any approach."""
    if approach.startswith('ESG-Fx_'):
        return discover_esgfx_suites(base_path, approach)
    elif approach.startswith('EFG_'):
        l_num = approach.split('_L')[1]
        return discover_efg_suites(base_path, int(l_num))
    else:
        print(f"  Unknown approach: {approach}")
        return {}