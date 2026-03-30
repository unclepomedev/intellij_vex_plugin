from extract_vex_globals import parse_global_variables


def test_parse_global_variables():
    vcc_output = """
    Context: sop

    Global Variables:
      (Read/Write)   vector P
      (Read Only)      int ptnum

    Control Statements:
      if ( condition )
    """

    result = parse_global_variables(vcc_output)

    assert len(result) == 2
    assert result[0] == {"name": "P", "type": "vector", "access": "Read/Write"}
    assert result[1] == {"name": "ptnum", "type": "int", "access": "Read Only"}


def test_parse_global_variables_empty():
    vcc_output = """
    Context: empty

    Functions:
      float Du( float; ... )
    """

    result = parse_global_variables(vcc_output)

    assert len(result) == 0
