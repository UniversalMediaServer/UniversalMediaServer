import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function HandFingerOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-hand-finger-off",
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
    d: "M8 13v-5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8.06 4.077a1.5 1.5 0 0 1 2.94 .423v2.5m0 4v1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12.063 8.065a1.5 1.5 0 0 1 1.937 1.435v.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14.06 10.082a1.5 1.5 0 0 1 2.94 .418v1.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M17 11.5a1.5 1.5 0 0 1 3 0v4.5m-.88 3.129a5.996 5.996 0 0 1 -5.12 2.871h-2h.208a6 6 0 0 1 -5.012 -2.7l-.196 -.3c-.312 -.479 -1.407 -2.388 -3.286 -5.728a1.5 1.5 0 0 1 .536 -2.022a1.867 1.867 0 0 1 2.28 .28l1.47 1.47"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { HandFingerOff as default };
