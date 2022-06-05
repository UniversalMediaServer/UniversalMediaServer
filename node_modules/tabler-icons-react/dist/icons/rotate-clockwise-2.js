import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function RotateClockwise2(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-rotate-clockwise-2",
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
    d: "M9 4.55a8 8 0 0 1 6 14.9m0 -4.45v5h5"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "5.63",
    y1: "7.16",
    x2: "5.63",
    y2: "7.17"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4.06",
    y1: "11",
    x2: "4.06",
    y2: "11.01"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "4.63",
    y1: "15.1",
    x2: "4.63",
    y2: "15.11"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7.16",
    y1: "18.37",
    x2: "7.16",
    y2: "18.38"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "11",
    y1: "19.94",
    x2: "11",
    y2: "19.95"
  }));
}

export { RotateClockwise2 as default };
