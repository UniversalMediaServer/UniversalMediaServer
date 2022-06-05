import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Microscope(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-microscope",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 21h14"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 18h2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7 18v3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 11l3 3l6 -6l-3 -3z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10.5 12.5l-1.5 1.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 3l3 3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 21a6 6 0 0 0 3.715 -10.712"
  }));
}

export { Microscope as default };
