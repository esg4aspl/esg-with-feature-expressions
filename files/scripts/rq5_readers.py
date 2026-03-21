#!/usr/bin/env python3
"""
rq5_readers.py — File readers for RQ5 Test Suite Redundancy Analysis
=====================================================================
Reads product configurations, ESG-Fx test suites (L0-L4), and EFG/GUITAR 
test suites (.tst XML + .EFG mapping).

All readers return a common representation:
  - sequences: set of tuples (each tuple is a sequence of event names)
  - edges: set of (event_a, event_b) tuples

This common format allows rq5_analysis.py to treat all approaches uniformly.
"""

import os
import re
import xml.etree.ElementTree as ET
from collections import OrderedDict


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
    """
    Strips trailing _N from event names used in L3/L4 files.
    'Single_Sign_On_1' -> 'Single_Sign_On'
    'pay' -> 'pay' (no suffix to strip)
    """
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
    
    L0/L1/L2 format: "N : event1, event2, event3"
    L3/L4 format:    "N : e1:e2:e3, e2:e3:e4, ..."  (colon-separated tuples with numeric IDs)
    
    Returns: (sequences: set of tuples, edges: set of (str, str))
    """
    sequences = set()
    edges = set()

    if not os.path.exists(filepath):
        return sequences, edges

    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            # Skip empty, coverage summary, and metadata lines
            if not line or re.match(r'^(L\d|Edge|Event|Random)', line):
                continue

            parts = line.split(' : ', 1)
            if len(parts) < 2:
                continue

            seq_data = parts[1].strip()
            if not seq_data:
                continue

            if level in ('L0', 'L1', 'L2'):
                events = [e.strip() for e in seq_data.split(', ') if e.strip()]
                seq_key = tuple(events)
                sequences.add(seq_key)
                edges |= _extract_edges_from_event_list(events)

            elif level in ('L3', 'L4'):
                tokens = [t.strip() for t in seq_data.split(', ') if t.strip()]
                
                # Reconstruct full event sequence from overlapping tuples
                all_events = []
                normalized_tokens = []

                for idx, token in enumerate(tokens):
                    event_parts = [_strip_numeric_suffix(p) for p in token.split(':')]
                    normalized_tokens.append(':'.join(event_parts))

                    # Extract edges within each tuple
                    for i in range(len(event_parts) - 1):
                        edges.add((event_parts[i], event_parts[i + 1]))

                    # Build full sequence: first tuple all events, then last event of each subsequent
                    if idx == 0:
                        all_events.extend(event_parts)
                    else:
                        all_events.append(event_parts[-1])

                seq_key = tuple(normalized_tokens)
                sequences.add(seq_key)

    return sequences, edges


def discover_esgfx_suites(base_path, approach):
    """
    Discovers and loads all ESG-Fx product test suites for a given approach.
    
    Approach format: "ESG-Fx_L0", "ESG-Fx_L1", ..., "ESG-Fx_L4"
    File patterns:
      L0: testsequences/L0/{product}_RandomWalk.txt
      L1-L4: testsequences/L{N}/{product}_L{N}.txt
    
    Returns: dict { product_name: (sequences_set, edges_set) }
    """
    suites = {}
    l_num = approach.split('_L')[1]
    level = f'L{l_num}'
    folder = os.path.join(base_path, 'testsequences', level)

    if not os.path.isdir(folder):
        return suites

    if l_num == '0':
        pattern = re.compile(r'^(P\d+)_RandomWalk\.txt$')
    else:
        pattern = re.compile(rf'^(P\d+)_L{l_num}\.txt$')

    for fname in sorted(os.listdir(folder)):
        match = pattern.match(fname)
        if match:
            product = match.group(1)
            filepath = os.path.join(folder, fname)
            seqs, edgs = read_esgfx_test_suite(filepath, level)
            if seqs or edgs:
                suites[product] = (seqs, edgs)

    return suites


# =============================================================================
# EFG / GUITAR TEST SUITE READERS
# =============================================================================

def read_efg_event_mapping(efg_filepath):
    """
    Reads .EFG XML file and builds EventId -> EventName mapping.
    Format: <Event><EventId>e2</EventId><Name>pay</Name>...</Event>
    
    Returns: dict { 'e2': 'pay', 'e3': 'change', ... }
    """
    mapping = {}
    if not os.path.exists(efg_filepath):
        return mapping

    try:
        tree = ET.parse(efg_filepath)
        root = tree.getroot()
        for event_elem in root.iter('Event'):
            event_id = event_elem.findtext('EventId', '').strip()
            # Handle both <Name> and <n> tags (varies by GUITAR version)
            event_name = event_elem.findtext('Name', '').strip()
            if not event_name:
                event_name = event_elem.findtext('n', '').strip()
            if event_id and event_name:
                mapping[event_id] = event_name
    except ET.ParseError as e:
        print(f"  Warning: Could not parse EFG file {efg_filepath}: {e}")

    return mapping


def read_guitar_tst_file(tst_filepath, event_mapping):
    """
    Reads a single GUITAR .tst XML file.
    Format: <TestCase><Step><EventId>e2</EventId>...</Step>...</TestCase>
    
    Returns: tuple of event names (e.g., ('pay', 'change', 'soda'))
    """
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
    """
    Reads a complete EFG test suite: .EFG mapping file + all .tst files in directory.
    
    Returns: (sequences: set of tuples, edges: set of (str, str))
    """
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
    
    Path pattern:
      EFG file: {base_path}/EFGs/{product}.EFG
      Test dir: {base_path}/EFGs/efg_testsequences/{product}/L{N}/
    
    Returns: dict { product_name: (sequences_set, edges_set) }
    """
    suites = {}
    efg_dir = os.path.join(base_path, 'EFGs')
    tst_base = os.path.join(base_path, 'EFGs', 'efg_testsequences')
    l_label = f'L{l_level}'

    if not os.path.isdir(efg_dir) or not os.path.isdir(tst_base):
        return suites

    # Find products that have both .EFG and test directories
    for product_dir in sorted(os.listdir(tst_base)):
        product_path = os.path.join(tst_base, product_dir, l_label)
        efg_path = os.path.join(efg_dir, f'{product_dir}.EFG')

        if os.path.isdir(product_path) and os.path.isfile(efg_path):
            seqs, edgs = read_efg_test_suite(efg_path, product_path)
            if seqs or edgs:
                # Normalize product name to match config format (e.g., P0001)
                suites[product_dir] = (seqs, edgs)

    return suites


# =============================================================================
# UNIFIED DISCOVERY
# =============================================================================

def discover_all_suites(base_path, approach):
    """
    Discovers and loads test suites for any approach.
    
    Supported approaches:
      - "ESG-Fx_L0" through "ESG-Fx_L4"
      - "EFG_L2" through "EFG_L4"
    
    Returns: dict { product_name: (sequences_set, edges_set) }
    """
    if approach.startswith('ESG-Fx_'):
        return discover_esgfx_suites(base_path, approach)
    elif approach.startswith('EFG_'):
        l_num = approach.split('_L')[1]
        return discover_efg_suites(base_path, int(l_num))
    else:
        print(f"  Unknown approach: {approach}")
        return {}